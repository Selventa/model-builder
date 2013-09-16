package model.builder.web.internal

import model.builder.web.api.APIManager
import model.builder.web.api.AccessInformation
import model.builder.web.api.AuthorizedAPI
import model.builder.web.api.OpenAPI

class DefaultAPIManager implements APIManager {

    final File configDir

    def authorizedAccess = [] as Set<AccessInformation>
    def openMap = [:] as Map<String, OpenAPI>

    DefaultAPIManager(File configDir) {
        this.configDir = configDir
        if (!configDir.exists()) {
            configDir.mkdirs()
        }
        read(configDir.listFiles().find {it.name == 'config.props'})
    }

    @Override
    AccessInformation getDefault() {
        authorizedAccess.find {it.defaultAccess}
    }

    @Override
    AuthorizedAPI authorizedAPI(String host) {
        def access = authorizedAccess.find {it.host == host}
        access ? new DefaultAuthorizedAPI(access) : null
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

    @Override
    public void saveConfiguration(Set<AccessInformation> accessSet) {
        authorizedAccess = accessSet
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
