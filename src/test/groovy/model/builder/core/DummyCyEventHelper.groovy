package model.builder.core

import org.cytoscape.event.CyEvent
import org.cytoscape.event.CyEventHelper
import org.cytoscape.event.CyPayloadEvent

public class DummyCyEventHelper implements CyEventHelper {
	private List<Object> lastEvents
	private LinkedList<Object> payloads
	private final boolean keepAllEvents

	public DummyCyEventHelper(boolean keepAllEvents) {
		this.keepAllEvents = keepAllEvents
		lastEvents = new ArrayList<Object>()
		payloads = new LinkedList<Object>()
	}

	public synchronized <E extends CyEvent<?>> void fireEvent(final E event) {
		if ( keepAllEvents || lastEvents.size() == 0 )
			lastEvents.add(event)
		else 
			lastEvents.set(0,event)
	}

	public <S,P,E extends CyPayloadEvent<S,P>> void addEventPayload(S source, P p, Class<E> e) {
		payloads.addLast(p)
	}

	public void silenceEventSource(Object o) {}

	public void unsilenceEventSource(Object o) {}

	public void flushPayloadEvents() {}
}
