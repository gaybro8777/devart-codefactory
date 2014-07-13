(ns thi.ng.cljs.detect
  (:require
   [goog.style :as style]))

(def mobile?
  (and (re-find #"(?i)mobile|tablet|ip(ad|hone|od)|android|silk" (.-userAgent js/navigator))
       (not (re-find #"(?i)crios" (.-userAgent js/navigator)))))

;; http://stackoverflow.com/questions/9847580

(def opera?
  (or (aget js/window "opera")
      (not (neg? (.indexOf (.-userAgent js/navigator) "OPR/")))))

(def firefox?
  (aget js/window "InstallTrigger"))

(def safari?
  (-> js/Object
      (.-prototype)
      (.-toString)
      (.call (aget js/window "HTMLElement"))
      (.indexOf "Constructor")
      (pos?)))

(def chrome? (and (aget js/window "chrome") (not opera?)))

(def ie? (or (aget js/document "documentMode")
             (re-find #"MSIE" (.-userAgent js/navigator))))
