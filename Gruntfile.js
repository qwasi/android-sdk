module.exports = function(grunt) {
    // Do grunt-related things in here

    var semverUtils = require('semver-utils');

    var isRelease = function() {
    	var package = grunt.file.readJSON('package.json');
        var semver = semverUtils.parse(package.version);
	
	return (semver.release.indexOf('SNAPSHOT') === -1);
    }

    grunt.initConfig({
	package: grunt.file.readJSON('package.json'),
	changelog: {
            'mojo-engine': {
                options: {
                    featureRegex: /^(.*)implements (MOJO-\d+)(.*)/gim,
                    fixRegex: /^(.*)fixes (MOJO-\d+)(.*)/gim,
                    dest: 'CHANGELOG.md',
                    template: '## Server Version <%= package.version %> / {{date}}\n\n{{> features}}{{> fixes}}',
                    partials: {
                        features: '{{#each features}}{{> feature}}{{/each}}',
                        feature: '- [NEW] {{this}}\n',
                        fixes: '{{#each fixes}}{{> fix}}{{/each}}',
                        fix: "- [FIX] {{this}}\n"
                    }
                }
            }
        },
	bump: {
	    options: {
		commit: true,
		commitMessage: '#bump v%VERSION%',
		commitFiles: ['-a'],
		createTag: true,
		tagName: 'v%VERSION%',
		tagMessage: 'Version %VERSION%',
		push: true,
		pushTo: 'origin',
		prereleaseName: isRelease() ? '' : 'SNAPSHOT',
		gitDescribeOptions: '--tags --always --abbrev=1 --dirty=-d',
		globalReplace: false
	    }
	}
    });

    grunt.loadNpmTasks('grunt-bump');
    grunt.loadNpmTasks('grunt-changelog');
};
