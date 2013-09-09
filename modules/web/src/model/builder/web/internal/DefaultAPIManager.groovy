package model.builder.web.internal

import model.builder.web.api.APIManager
import model.builder.web.api.AccessInformation
import model.builder.web.api.AuthorizedAPI
import model.builder.web.api.OpenAPI

class DefaultAPIManager implements APIManager {

    final File configDir

    def authorizedAccess = [] as Set<AccessInformation>
    def openMap = [:] as Map<String, OpenAPI>
    def AccessInformation defaultAccess

    DefaultAPIManager(File configDir) {
        this.configDir = configDir
        if (!configDir.exists()) {
            configDir.mkdirs()
        }
        read(configDir.listFiles().find {it.name == 'config.props'})
    }

    @Override
    AccessInformation getDefault() {
        defaultAccess
    }

    @Override
    void setDefault(AccessInformation access) {
        if (! access in authorizedAccess)
            throw new IllegalStateException("$access must be added first")
        defaultAccess = access
    }

    @Override
    AccessInformation authorizedAccess(String host) {
        authorizedAccess.find{it.host = host}
    }

    @Override
    AuthorizedAPI authorizedAPI(AccessInformation access) {
        new DefaultAuthorizedAPI(access)
    }

    @Override
    OpenAPI openAPI(String host) {
        openMap[host] ?: (openMap[host] = new DefaultOpenAPI(host))
    }

    @Override
    void add(AccessInformation access) {
        authorizedAccess.add(access)
    }

    @Override
    void remove(AccessInformation access) {
        authorizedAccess.remove(access)
    }

    @Override
    Set<AccessInformation> all() {
        authorizedAccess
    }

    public void saveConfiguration() {
        write(new File(configDir, 'config.props'))
    }

    private void read(File configFile) {
        if (configFile) {
            Properties props = new Properties()
            props.load(new FileInputStream(configFile));
            authorizedAccess = props.collect { k, v ->
                def (defaultAccess, host, email, apiKey, privateKey) = v.toString().split(/,/)
                new AccessInformation(defaultAccess.toBoolean(), host, email, apiKey, privateKey)
            }
            defaultAccess = authorizedAccess.findAll {it.defaultAccess}.last()
        }
    }

    private void write(File configFile) {
        if (configFile) {
            Properties props = new Properties()
            int c = 1
            authorizedAccess.each {
                props.setProperty("server.${c++}", it.configValue)
            }
            props.store(new FileOutputStream(configFile), null)
        }
    }
}
