package com.aimmac23.hub.servlet;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.exec.StreamPumper;
import org.apache.http.HttpStatus;
import org.openqa.grid.internal.ExternalSessionKey;

import com.aimmac23.hub.HubVideoRegistry;
import com.aimmac23.hub.videostorage.StoredVideoDownloadContext;
import com.aimmac23.hub.videostorage.StoredVideoInfoContext;

/**
 * A servlet to download videos for a given sessionId.
 * 
 * Note that videos are not available until you have closed the Selenium Session
 * (calling driver.quit(), for example).
 * 
 * @author Alasdair Macmillan
 *
 */
public class HubVideoDownloadServlet extends AbstractHubVideoServlet {
	
	private static final Logger log = Logger.getLogger(HubVideoDownloadServlet.class.getName());

	private static final long serialVersionUID = 1L;
	
	static {
		try {
			// force this class to be initialized, so any errors are thrown at startup instead of first use
			Class.forName(HubVideoRegistry.class.getCanonicalName());
		} 
		catch (ClassNotFoundException e) {
			// Can't happen
		}
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String sessionId = req.getParameter("sessionId");
		
		if(sessionId == null) {
			resp.setStatus(HttpStatus.SC_BAD_REQUEST);
			resp.getWriter().write("Missing parameter: 'sessionId'");
			return;
		}
		
		if(!checkValidSessionId(sessionId, resp)) {
			// response writing already handled
			return;
		}
		
		StoredVideoDownloadContext videoContext;
		try {
			videoContext = HubVideoRegistry.getVideoForSession(new ExternalSessionKey(sessionId));
		} catch (Exception e) {
			log.log(Level.WARNING, "Caught exception when fetching video for " + sessionId, e);
			resp.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			resp.getWriter().write("Internal error when fetching video");
			return;
		}
		
		if(!videoContext.isVideoFound()) {
			resp.setStatus(HttpStatus.SC_NOT_FOUND);
			resp.getWriter().write("Video content not found for sessionId: " + sessionId);
			videoContext.close();
			return;
		}
		
		try {
			resp.setContentType("video/webm");
			
			Long contentLength = videoContext.getContentLengthIfKnown();
			if(contentLength != null) {
				resp.setContentLength(contentLength.intValue());

			}
			new StreamPumper(videoContext.getStream(), resp.getOutputStream()).run();
			return;
		}
		finally {
			videoContext.close();
		}
	}
	
	@Override
	protected void doHead(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String sessionId = req.getParameter("sessionId");
		
		if(sessionId == null) {
			resp.setStatus(HttpStatus.SC_BAD_REQUEST);
			return;
		}
		
		StoredVideoInfoContext videoInfoForSession;
		try {
			videoInfoForSession = HubVideoRegistry.getVideoInfoForSession(new ExternalSessionKey(sessionId));
		} catch (Exception e) {
			log.log(Level.WARNING, "Caught exception when fetching video information for " + sessionId, e);
			resp.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			resp.getWriter().write("Internal error when fetching video information");
			return;
		}
		
		if(!videoInfoForSession.isVideoFound()) {
			resp.setStatus(HttpStatus.SC_NOT_FOUND);
			return;
		}
		
		resp.setStatus(HttpStatus.SC_OK);
		resp.setContentType("video/mp4");
		if(videoInfoForSession.getContentLengthIfKnown() != null) {
			resp.setContentLength(videoInfoForSession.getContentLengthIfKnown().intValue());
		}
		return;
	}
}
