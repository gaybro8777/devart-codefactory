/*global module:false*/
module.exports = function(grunt) {

  // Project configuration.
  grunt.initConfig({
    // Metadata.
    pkg: grunt.file.readJSON('package.json'),
    less: {
      dev: {
        options: {
          paths: "src-less",
          compress: true
        },
        files: {
          "war/staging/css/app.css": "src-less/main.less"
        }
      },
      prod: {
        options: {
          paths: "src-less",
          compress: true,
        },
        files: {
          "war/css/app.css": "src-less/main.less"
        }
      }
    },
    htmlmin: {
      dev: {
        options: {
          removeComments: true,
          collapseWhitespace: true,
          keepClosingSlash: true,
          caseSensitive: true,
          minifyJS: true
        },
        files: {
          'war/staging/index.html': 'src-html/staging.html'
        }
      },
      prod: {
        options: {
          removeComments: true,
          collapseWhitespace: true,
          keepClosingSlash: true,
          caseSensitive: true,
          minifyJS: true
        },
        files: {
          'war/index.html': 'src-html/index.html',
          'war/barbican/index.html': 'src-html/barbican.html',
          'war/workshop/index.html': 'src-html/workshop.html',
          'war/staging/index.html': 'src-html/staging.html'
        }
      }
    },
    watch: {
      dev: {
        files: ['src-less/*.less','src-html/*.html'],
        tasks: ['less:dev','htmlmin:dev']
      },

      prod: {
        files: ['src-less/*.less','src-html/*.html'],
        tasks: ['less:prod','htmlmin:prod']
      }
    },
    replace: {
      dist: {
        options: {
          patterns: [
            {
              match: 'timestamp',
              replacement: '<%= new Date().getTime() %>'
            }
          ]
        },
        files: [
          {src: ['war/index.html'], dest: 'war/index.html'},
          {src: ['war/barbican/index.html'], dest: 'war/barbican/index.html'},
          {src: ['war/workshop/index.html'], dest: 'war/workshop/index.html'},
          {src: ['war/staging/index.html'], dest: 'war/staging/index.html'},
        ]
      }
    }
  });

  // These plugins provide necessary tasks.
  grunt.loadNpmTasks('grunt-contrib-concat');
  grunt.loadNpmTasks('grunt-contrib-less');
  grunt.loadNpmTasks('grunt-contrib-watch');
  grunt.loadNpmTasks('grunt-contrib-htmlmin');
  //grunt.loadNpmTasks('grunt-contrib-copy');
  grunt.loadNpmTasks('grunt-replace');

  // Default task.
  grunt.registerTask('default', ['less:dev','htmlmin:dev','watch:dev']);
};
