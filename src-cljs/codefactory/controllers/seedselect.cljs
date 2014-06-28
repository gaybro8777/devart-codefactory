(ns codefactory.controllers.seedselect
  (:require-macros
   [thi.ng.macromath.core :as mm])
  (:require
   [codefactory.config :as config]
   [codefactory.color :as col]
   [codefactory.webgl :as webgl]
   [codefactory.protocols :as proto]
   [codefactory.controllers.shared :as shared]
   [thi.ng.cljs.log :refer [debug info warn]]
   [thi.ng.cljs.app :as app :refer [handle-event emit]]
   [thi.ng.cljs.dom :as dom]
   [thi.ng.cljs.io :as io]
   [thi.ng.cljs.route :as route]
   [thi.ng.geom.webgl.core :as gl]
   [thi.ng.geom.webgl.animator :as anim]
   [thi.ng.geom.webgl.buffers :as buf]
   [thi.ng.geom.core :as g]
   [thi.ng.geom.core.matrix :as mat :refer [M44]]
   [thi.ng.geom.core.vector :as v :refer [vec2 vec3]]
   [thi.ng.geom.rect :as r]
   [thi.ng.geom.basicmesh :as bm]
   [thi.ng.geom.ui.arcball :as arcball]
   [thi.ng.morphogen.core :as mg]
   [thi.ng.common.math.core :as m]
   ))

(declare instance)

(defn init-meshes
  [state]
  (let [gl (:gl state)
        meshes (reduce
                (fn [acc [id {:keys [seed x]}]]
                  (conj acc
                        {:mesh (-> (g/into (bm/basic-mesh) (g/faces seed))
                                   (gl/as-webgl-buffer-spec {:tessellate true :fnormals true})
                                   (buf/make-attribute-buffers-in-spec gl gl/static-draw))
                         :id id}))
                [] config/seeds)]
    (merge state {:meshes meshes})))

(defn mesh-spec-for-id
  [meshes id]
  (let [num (count meshes)
        id (rem id num)]
    (meshes (if (neg? id) (+ id num) id))))

(defn render-scene
  [state]
  (if (:active? @state)
    (let [{:keys [gl shaders proj meshes selection cam-x time]} @state
          cam-x (m/mix cam-x (* selection 2) 0.15)
          view (mat/look-at (vec3 cam-x -3 1) (vec3 cam-x 0 0) (vec3 0 0 1))
          shared-unis {:view view
                       :proj proj}
          num (count meshes)
          sel (rem selection num)
          sel (if (neg? sel) (+ sel num) sel)]
      (apply gl/clear-color-buffer gl (:bg-col config/webgl))
      (gl/clear-depth-buffer gl 1.0)
      (loop [i (- sel 2), x (mm/msub selection 2 4)]
        (when (<= i (+ sel 2))
          (let [{:keys [mesh]} (meshes (if (neg? i) (+ i num) (rem i num)))
                sel? (== i sel)
                model-mat (g/translate M44 x 0 0)
                model-mat (if sel? (g/rotate-z model-mat (* time -2)) model-mat)
                norm-mat (-> (g/* view model-mat) g/invert g/transpose)
                shared-unis (assoc shared-unis :model model-mat :normalMat norm-mat)]
            (if sel?
              (webgl/render-meshes gl (shaders 1) {:a mesh} shared-unis nil)
              (webgl/render-meshes gl (shaders 0) {:a mesh} shared-unis {:alpha 0.5}))
            (recur (inc i) (+ x 2)))))
      (app/merge-state state {:time (+ time 0.01666) :cam-x cam-x})
      (anim/animframe-provider (fn [& _] (render-scene state))))))

(defn update-overlay
  [e]
  (if-not (:user-dragged? @(.-state instance))
    (let [x (.-clientX e)
          y (.-clientY e)]
      (dom/set-style!
       (dom/by-id "seed-overlay")
       #js {:left (str (- x 100) "px") :top (str (- y 30) "px")
            :display "block"}))))

(defn start-editor
  []
  (let [state (.-state instance)
        {:keys [meshes selection]} @state
        spec (mesh-spec-for-id meshes selection)]
    (swap! state assoc :active? false)
    (route/set-route! "edit" "new" (name (:id spec)))))

(defn switch-seed
  [e]
  (let [state (.-state instance)]
    (case (.-keyCode (.getBrowserEvent e))
      39 (swap! state update-in [:selection] inc)
      37 (swap! state update-in [:selection] dec)
      nil)))

(defn init-state
  [state queue initial opts]
  (let [resize-window (shared/resize-window* state initial (fn [& _] ))
        dom-listeners [["#seed-cancel" "click" (shared/cancel-module "home")]
                       ["#seed-continue" "click" start-editor]
                       ["#seed-canvas" "mousemove" update-overlay]
                       ["$window" "resize" resize-window]
                       ["$window" "keydown" switch-seed]]
        h-listeners [["drag" (fn [e]
                               (let [g (.-gesture e)
                                     dx (.-deltaX g)]
                                 (if (and (not (:drag-switch @state))
                                          (> (.-deltaTime g) 150)
                                          (> (Math/abs dx) 60))
                                   (let [s (:selection @state)]
                                     (if-not (:user-dragged? @state)
                                       (dom/set-style! (dom/by-id "seed-overlay") #js {:display "none"}))
                                     (app/merge-state state {:selection (if (neg? dx) (dec s) (inc s))
                                                             :drag-switch true
                                                             :user-dragged? true})))))]
                     ["dragend" (fn [e] (swap! state assoc-in [:drag-switch] false))]]
        hammer (js/Hammer (.-body js/document))]
    (reset!
     state
     (-> initial
         (merge
          {:dom-listeners dom-listeners
           :h-listeners h-listeners
           :hammer hammer
           :selection 0
           :cam-x 0
           :time 0
           :active? true
           :drag-switch false
           :user-dragged? false})
         (init-meshes)))
    (resize-window)
    (app/add-listeners dom-listeners)
    (app/add-hammer-listeners hammer h-listeners)
    (render-scene state)))

(deftype SeedController
    [state ^:mutable shared ^:mutable queue]
  proto/PController
  (init [_ opts]
    (debug :init-seedsel)
    (set! shared (:state opts))
    (set! queue (:queue opts))
    (if-let [initial (webgl/init-webgl (dom/by-id "seed-canvas"))]
      (init-state state queue initial opts)
      (app/emit queue :webgl-missing nil)))
  (release [_]
    (debug :release-seedselect)
    (app/remove-listeners (:dom-listeners @state))
    (app/remove-hammer-listeners (:hammer @state) (:h-listeners @state))
    (dom/set-style! (dom/by-id "seed-overlay") #js {:display "none"})
    (reset! state nil)
    (set! shared nil)))

(def instance (SeedController. (atom nil) nil nil))
