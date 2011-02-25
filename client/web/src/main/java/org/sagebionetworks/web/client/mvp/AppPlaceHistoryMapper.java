package org.sagebionetworks.web.client.mvp;

import org.sagebionetworks.web.client.place.DatasetsHome;
import org.sagebionetworks.web.client.place.Dataset;

import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.WithTokenizers;

/**
 * PlaceHistoryMapper interface is used to attach all places which the
 * PlaceHistoryHandler should be aware of. This is done via the @WithTokenizers
 * annotation or by extending PlaceHistoryMapperWithFactory and creating a
 * separate TokenizerFactory.
 */
@WithTokenizers( { DatasetsHome.Tokenizer.class, Dataset.Tokenizer.class})
public interface AppPlaceHistoryMapper extends PlaceHistoryMapper {
}
