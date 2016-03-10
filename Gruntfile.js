module.exports = function(grunt) {
    // Do grunt-related things in here

    var semverUtils = require('semver-utils');

    var isRelease = function() {
    	var package = grunt.file.readJSON('package.json');
        var semver = semverUtils.parse(package.version);
	
	return (semver.release.indexOf('beta') === -1);
    }

    grunt.initConfig({
	package: grunt.file.readJSON('package.json'),
	changelog: {
            'Qwasi Android SDK': {
                options: {
                    featureRegex: /^(.*)implements (\[DROID-\d+\])(.*)/gim,
                    fixRegex: /^(.*)/gim,
                    after: 'bf2c26a29b7',
                    dest: 'CHANGELOG.md',
                    template: '## SDK Version <%= package.version %> / {{date}}\n\n{{> features}}{{> fixes}}',
                    partials: {
                        features: '{{#each features}}{{> feature}}{{/each}}',
                        feature: '- [NEW] {{this}}\n',
                        fixes: '{{#each fixes}}{{> fix}}{{/each}}',
                        fix: "- [FIX] {{this}}\n"
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
		prereleaseName: isRelease() ? '' : 'beta',
		gitDescribeOptions: '--tags --always --abbrev=1 --dirty=-d',
		globalReplace: false
	    }
	}
    });

    grunt.loadNpmTasks('grunt-bump');
    grunt.loadNpmTasks('grunt-changelog');
};
