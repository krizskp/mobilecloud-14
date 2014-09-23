/*
 * 
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package org.magnum.dataup;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class VideoSvcController {

	/**
	 * You will need to create one or more Spring controllers to fulfill the
	 * requirements of the assignment. If you use this file, please rename it
	 * to something other than "AnEmptyController"
	 * 
	 * 
		 ________  ________  ________  ________          ___       ___  ___  ________  ___  __       
		|\   ____\|\   __  \|\   __  \|\   ___ \        |\  \     |\  \|\  \|\   ____\|\  \|\  \     
		\ \  \___|\ \  \|\  \ \  \|\  \ \  \_|\ \       \ \  \    \ \  \\\  \ \  \___|\ \  \/  /|_   
		 \ \  \  __\ \  \\\  \ \  \\\  \ \  \ \\ \       \ \  \    \ \  \\\  \ \  \    \ \   ___  \  
		  \ \  \|\  \ \  \\\  \ \  \\\  \ \  \_\\ \       \ \  \____\ \  \\\  \ \  \____\ \  \\ \  \ 
		   \ \_______\ \_______\ \_______\ \_______\       \ \_______\ \_______\ \_______\ \__\\ \__\
		    \|_______|\|_______|\|_______|\|_______|        \|_______|\|_______|\|_______|\|__| \|__|
                                                                                                                                                                                                                                                                        
	 * 
	 */
	
	// ---------------------------------------------------------------------------------------------
	public static final String DATA_PARAMETER = "data";
	public static final String ID_PARAMETER = "id";
	public static final String VIDEO_SVC_PATH = "/video";
	public static final String VIDEO_DATA_PATH = VIDEO_SVC_PATH + "/{id}/data";
	
	private static final AtomicLong currentId = new AtomicLong(0L);
	private VideoFileManager videoDataManager;
	
	// Video data storage
	private Map<Long, Video> videoList = new HashMap<Long, Video>();
	
	public VideoSvcController() throws IOException {
		videoDataManager = VideoFileManager.get();
	}
	
	@RequestMapping(value=VIDEO_SVC_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList() {
		return videoList.values();
	}
	
	@RequestMapping(value=VIDEO_SVC_PATH, method=RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v) {
		checkAndSetId(v);
		v.setDataUrl(getDataUrl(v.getId()));
		videoList.put(v.getId(), v);
		return v;
	}

	@RequestMapping(value=VIDEO_DATA_PATH, method=RequestMethod.POST)
	public @ResponseBody VideoStatus setVideoData(@PathVariable(ID_PARAMETER) long id, 
			@RequestParam(DATA_PARAMETER) MultipartFile videoData, HttpServletResponse response) throws IOException {
		VideoStatus status = new VideoStatus(VideoState.PROCESSING);
		if (id == 0 || !videoList.containsKey(id)) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		} else {
			Video v = videoList.get(id);
			saveVideoData(v, videoData);
			status.setState(VideoState.READY);
		}
		
		return status;
	}

	@RequestMapping(value=VIDEO_DATA_PATH, method=RequestMethod.GET)
	public void getData(@PathVariable(ID_PARAMETER) long id, HttpServletResponse response) throws IOException{
		if (id == 0 || !videoList.containsKey(id)) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		} else {
			Video v = videoList.get(id);
			try {
				serveVideoData(v, response);
			} catch (IOException e) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST);
				e.printStackTrace();
			}
		}
	}
	
	
	// ---------------------------------------------------------------------------------------------
	
	private void checkAndSetId(Video v) {
		if (v.getId() == 0) {
			v.setId(currentId.incrementAndGet());
		}
	}
	
	private String getDataUrl(long videoId) {
		String url = getUrlFromBaseForLocalServer() + "/video/" + videoId + "/data";
		return url;
	}

	private String getUrlFromBaseForLocalServer() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		String base = "http://" + request.getServerName() + ((request.getServerPort() != 80) ? ":" + request.getServerPort() : "");
		return base;
	}
	
	public void saveVideoData(Video v, MultipartFile videoData) throws IOException {
        videoDataManager.saveVideoData(v, videoData.getInputStream());
   }
	
	public void serveVideoData(Video v, HttpServletResponse response) throws IOException {
        videoDataManager.copyVideoData(v, response.getOutputStream());
   }
	
}
