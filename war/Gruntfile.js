module.exports = function (grunt) {

  // load all grunt tasks
  require('matchdep').filterDev('grunt-*').forEach(grunt.loadNpmTasks);

  grunt.initConfig({
    uglify: {
      dist: {
        files: {
          'target/jenkins/scripts/jenkins.js': [
            'src/main/webapp/scripts/*.js'
          ]
        }
      }
    },
    rev: {
      dist: {
        files: {
          src: [
            'target/jenkins/scripts/jenkins.js',
            'target/jenkins/scripts/yui/{,**/}*.js'
          ]
        }
      }
    }
  });

  grunt.registerTask('build', [
      'uglify',
      'rev'
  ]);

  grunt.registerTask('default', ['build']);
};
