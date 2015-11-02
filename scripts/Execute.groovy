includeTargets << grailsScript('_GrailsBootstrap')


target(main: "The description of the script goes here!") {
//    depends checkVersion, configureProxy, compile, bootstrap, runScript
//    depends checkVersion, configureProxy, compile,  runScript
    depends configureProxy, compile, runScript
}

target(runScript: 'Main implementation that executes the specified script(s) after starting up the application environment') {

    if (!argsMap.params) {
        event('StatusError', ['ERROR: Required script name parameter is missing'])
        System.exit 1
    }

    for (scriptFile in argsMap.params) {
        event('StatusUpdate', ["Running script $scriptFile ..."])
        executeScript scriptFile, classLoader
        event('StatusUpdate', ["Script $scriptFile complete!"])
    }
}


def executeScript(scriptFile, classLoader) {
    File script = new File(scriptFile)
    if (!script.exists()) {
        event('StatusError', ["Designated script doesn't exist: $scriptFile"])
        return
    }

//    def shell = new GroovyShell(classLoader, new Binding(ctx: appCtx, grailsApplication: grailsApp))
    def shell = new GroovyShell(classLoader, new Binding())

    shell.evaluate script.getText('UTF-8')
}


setDefaultTarget(main)
