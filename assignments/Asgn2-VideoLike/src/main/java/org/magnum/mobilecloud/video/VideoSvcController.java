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

package org.magnum.mobilecloud.video;

import java.io.IOException;
import java.security.Principal;
import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

import org.magnum.mobilecloud.video.repository.Video;
import org.magnum.mobilecloud.video.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Lists;

@Controller
public class VideoSvcController {

	/**
	 * You will need to create one or more Spring controllers to fulfill the
	 * requirements of the assignment. If you use this file, please rename it to
	 * something other than "AnEmptyController"
	 * 
	 * 
	 * ________ ________ ________ ________ ___ ___ ___ ________ ___ __ |\
	 * ____\|\ __ \|\ __ \|\ ___ \ |\ \ |\ \|\ \|\ ____\|\ \|\ \ \ \ \___|\ \
	 * \|\ \ \ \|\ \ \ \_|\ \ \ \ \ \ \ \\\ \ \ \___|\ \ \/ /|_ \ \ \ __\ \ \\\
	 * \ \ \\\ \ \ \ \\ \ \ \ \ \ \ \\\ \ \ \ \ \ ___ \ \ \ \|\ \ \ \\\ \ \ \\\
	 * \ \ \_\\ \ \ \ \____\ \ \\\ \ \ \____\ \ \\ \ \ \ \_______\ \_______\
	 * \_______\ \_______\ \ \_______\ \_______\ \_______\ \__\\ \__\
	 * \|_______|\|_______|\|_______|\|_______|
	 * \|_______|\|_______|\|_______|\|__| \|__|
	 * 
	 * 
	 */

	// ----------------------------------------------------------------------------------------------

	// Video Repository
	@Autowired
	private VideoRepository videos;

	// Paths and Parameters
	public static final String TITLE_PARAMETER = "title";
	public static final String DURATION_PARAMETER = "duration";
	public static final String ID_PARAMETER = "id";
	public static final String TOKEN_PATH = "/oauth/token";
	// The path where we expect the VideoSvc to live
	public static final String VIDEO_SVC_PATH = "/video";
	// The path to search videos by title
	public static final String VIDEO_TITLE_SEARCH_PATH = VIDEO_SVC_PATH
			+ "/search/findByName";
	// The path to search videos by title
	public static final String VIDEO_DURATION_SEARCH_PATH = VIDEO_SVC_PATH
			+ "/search/findByDurationLessThan";
	public static final String VIDEO_LIKE_PATH = VIDEO_SVC_PATH + "/{id}/like";
	public static final String VIDEO_UNLIKE_PATH = VIDEO_SVC_PATH + "/{id}/unlike";
	public static final String VIDEO_LIKED_BY_PATH = VIDEO_SVC_PATH + "/{id}/likedby";
	
	// ----------------------------------------------------------------------------------------------

	@RequestMapping(value = "/go", method = RequestMethod.GET)
	public @ResponseBody String goodLuck() {
		return "Good Luck!";
	}

	@RequestMapping(value = VIDEO_SVC_PATH, method = RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList() {
		return Lists.newArrayList(videos.findAll());
	}

	@RequestMapping(value = VIDEO_SVC_PATH + "/{id}", method = RequestMethod.GET)
	public @ResponseBody Video getVideoById(@PathVariable(ID_PARAMETER) long id, HttpServletResponse response) throws IOException {
		Video v = videos.findOne(id);
		
		if (v == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Video not found!.");
			return null;
		}
		return v;
	}

	@RequestMapping(value = VIDEO_SVC_PATH, method = RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v) {
		return videos.save(v);
	}

	@RequestMapping(value = VIDEO_LIKE_PATH, method = RequestMethod.POST)
	public void likeVideo(@PathVariable(ID_PARAMETER) long id, Principal p, HttpServletResponse response) throws IOException {
		Video v = videos.findOne(id);
		
		// Check if video exists
		if (v == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "The video doesn't exist");
		} else {
			Collection<String> likers = v.getLikedBy();
			// Check if video has already been liked
			if (likers.contains(p.getName())) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "You have already liked this video");
			} else {
				likers.add(p.getName());
				v.setLikedBy(likers);
				videos.save(v);
				response.sendError(HttpServletResponse.SC_OK, "Video liked");
			}
		}
	}

	@RequestMapping(value=VIDEO_UNLIKE_PATH, method=RequestMethod.POST)
	public void unlikeVideo(@PathVariable(ID_PARAMETER) long id, Principal p, HttpServletResponse response) throws IOException {
		Video v = videos.findOne(id);
		
		// Check if video exists
		if (v == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Video not found");
		} else {
			Collection<String> likers = v.getLikedBy();
			// Check if video was not liked
			if (likers.contains(p.getName())) {
				likers.remove(p.getName());
				v.setLikedBy(likers);
				videos.save(v);
				response.sendError(HttpServletResponse.SC_OK, "Video unliked");
			} else {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "You haven't liked this video");
			}
		}
	}

	@RequestMapping(value = VIDEO_TITLE_SEARCH_PATH, method = RequestMethod.GET)
	public @ResponseBody Collection<Video> findByTitle(@RequestParam(TITLE_PARAMETER) String title) {
		return Lists.newArrayList(videos.findByName(title));
	}

	@RequestMapping(value = VIDEO_DURATION_SEARCH_PATH, method = RequestMethod.GET)
	public @ResponseBody Collection<Video> findByDurationLessThan(@RequestParam(DURATION_PARAMETER) long duration) {
		return Lists.newArrayList(videos.findByDurationLessThan(duration));
	}

	@RequestMapping(value=VIDEO_LIKED_BY_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<String> getUsersWhoLikedVideo(
			@PathVariable(ID_PARAMETER) long id, HttpServletResponse response) throws IOException {
		Video v = videos.findOne(id);
		
		// Check if video exists
		if (v == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Video not found!");
			return null;
		}
		
		return v.getLikedBy();
	}

}
