package org.sagebionetworks.repo.manager.message.markdown;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.sagebionetworks.repo.manager.message.markdown.MarkdownClientException;

/**
 * Abstract for interacting with Markdown Server: http://markdownit.prod.sagebase.org
 */
public interface MarkdownDao {

	/**
	 * Send a request to the Markdown Server to convert the rawMarkdown to the output type.
	 * If the output type is not specified, the raw markdown will be converted to html with
	 * Synapse style.
	 * 
	 * @param rawMarkdown
	 * @param outputType
	 * @return
	 * @throws IOException 
	 * @throws ClientProtocolException
	 * @throws JSONException 
	 * @throws org.sagebionetworks.markdown.MarkdownClientException
	 */
	String convertMarkdown(String rawMarkdown, String outputType) throws ClientProtocolException, IOException, JSONException, MarkdownClientException;
}
