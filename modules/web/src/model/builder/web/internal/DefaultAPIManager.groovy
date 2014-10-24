package model.builder.web.internal

import model.builder.web.api.APIManager
import model.builder.web.api.AccessInformation
import model.builder.web.api.AuthorizedAPI
import model.builder.web.api.OpenAPI
import model.builder.web.api.event.AddedAccessInformationEvent
import model.builder.web.api.event.RemovedAccessInformationEvent
import model.builder.web.api.event.SetDefaultAccessInformationEvent
import org.cytoscape.event.CyEventHelper
import org.openbel.ws.api.WsManager

import static String.format;

class DefaultAPIManager implements APIManager {

    static final String SDP_OPENBEL_URL = '%s://%s/openbel-ws/belframework/'

    final File configDir
    final WsManager wsManager

    Set<AccessInformation> authorizedAccess = [] as Set<AccessInformation>
    Map<String, OpenAPI> openMap = [:] as Map<String, OpenAPI>
    CyEventHelper evtHelper

    DefaultAPIManager(File configDir, CyEventHelper evtHelper, WsManager wsManager = null) {
        this.configDir = configDir
        this.evtHelper = evtHelper
        this.wsManager = wsManager
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
    AuthorizedAPI byHost(String host) {
        def access = authorizedAccess.find {it.host == host}
        access ? new DefaultAuthorizedAPI(access) : null
    }

    @Override
    AuthorizedAPI byAccess(AccessInformation access) {
        access ? new DefaultAuthorizedAPI(access) : null
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
        authorizedAccess.collect { it.clone() }
    }

    @Override
    public void saveConfiguration(Set<AccessInformation> accessSet) {
        // fire changes
        fireEventsOnChange(evtHelper, this, authorizedAccess, accessSet)

        // stateful change
        authorizedAccess = accessSet

        // save
        write(new File(configDir, 'config.props'))
        syncWsManager()
    }

    protected static void fireEventsOnChange(CyEventHelper evtHelper,
                                             APIManager mgr,
                                             Set<AccessInformation> cur,
                                             Set<AccessInformation> next) {
        next.minus(cur).each {
            evtHelper.fireEvent(new AddedAccessInformationEvent(mgr, it))
        }
        cur.minus(next).each {
            evtHelper.fireEvent(new RemovedAccessInformationEvent(mgr, it))
        }

        AccessInformation curDefault = cur.find { it.defaultAccess }
        AccessInformation nextDefault = next.find { it.defaultAccess }
        if (!nextDefault.equals(curDefault)) {
            evtHelper.fireEvent(new SetDefaultAccessInformationEvent(mgr, curDefault, nextDefault))
        }
    }

    private void read(File configFile) {
        if (configFile) {
            Properties props = new Properties()
            props.load(new FileInputStream(configFile));
            authorizedAccess = props.collect { k, v ->
                def (defaultAccess, host, email, apiKey, privateKey) = v.toString().split(/,/)
                new AccessInformation(defaultAccess.toBoolean(), host, email, apiKey, privateKey)
            }
            syncWsManager()
        }
    }

    private void syncWsManager() {
        if (wsManager) {
            all().collect {
                belURI(it.host)
            }.each {
                wsManager.add(new URI(it))
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

    private static String belURI(String host) {
        if (host == 'localhost')
            format(SDP_OPENBEL_URL, 'http', (host + ':8080'))
        else
            format(SDP_OPENBEL_URL, 'https', host)
    }
}
