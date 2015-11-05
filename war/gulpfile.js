//
// See https://github.com/tfennelly/jenkins-js-builder
//
var builder = require('jenkins-js-builder');

//
// Bundle the Install Wizard.
// See https://github.com/tfennelly/jenkins-js-builder#bundling
//
builder.bundle('src/main/js/pluginSetupWizard.js')
    .withExternalModuleMapping('jquery-detached', 'core:jquery2')
    .withExternalModuleMapping('bootstrap', 'core:bootstrap3')
    .withExternalModuleMapping('handlebars', 'core:handlebars3')
    .less('src/main/less/pluginSetupWizard.less')
    .inDir('target/classes/assets');
