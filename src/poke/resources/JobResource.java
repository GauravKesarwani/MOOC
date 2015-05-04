/*
 * copyright 2012, gash
 * 
 * Gash licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package poke.resources;

import java.util.List;

import mooc.storage.Course;
import mooc.storage.CourseRiak;
import mooc.storage.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.resources.Resource;
import poke.server.resources.ResourceUtil;

import com.basho.riak.client.RiakException;
import com.basho.riak.client.RiakRetryFailedException;
import com.basho.riak.client.cap.UnresolvedConflictException;
import com.basho.riak.client.convert.ConversionException;

import eye.Comm.GetCourse;
import eye.Comm.Payload;
import eye.Comm.PokeStatus;
import eye.Comm.Request;
import eye.Comm.RequestList;

public class JobResource implements Resource {
	protected static Logger logger = LoggerFactory.getLogger("server");

	@Override
	public Request process(Request request) {
		logger.info("JOB: " + request.getHeader().getTag());
		Request.Builder rb = Request.newBuilder();
		Request reply;

		// Sign Up
		if (request.getHeader().getTag().equalsIgnoreCase("signup")) {
			boolean isExist = true;
			User user = new User();
			user.setCredentials(request.getBody().getSignUp().getUserName(),
					request.getBody().getSignUp().getPassword());

			try {
				isExist = user.isuserName(request.getBody().getSignUp()
						.getUserName());
			} catch (UnresolvedConflictException e) {
				e.printStackTrace();
			} catch (RiakRetryFailedException e) {
				e.printStackTrace();
			} catch (ConversionException e) {
				e.printStackTrace();
			}

			// metadata
			if (isExist) {
				rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						PokeStatus.SUCCESS, "Sucessfully signed up"));

			} else {
				rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						PokeStatus.FAILURE,
						"Username already exist. Please try with another username. "));
			}
			// payload
			Payload.Builder pb = Payload.newBuilder();
			rb.setBody(pb.build());

			reply = rb.build();
			return reply;
		}

		// Sign In
		if (request.getHeader().getTag().equalsIgnoreCase("SignIn")) {
			boolean validUser = false;
			User user = new User();
			try {
				validUser = user.signIn(request.getBody().getSignIn()
						.getUserName(), request.getBody().getSignIn()
						.getPassword());
			} catch (UnresolvedConflictException e1) {
				e1.printStackTrace();
			} catch (RiakRetryFailedException e1) {
				e1.printStackTrace();
			} catch (ConversionException e1) {
				e1.printStackTrace();
			}

			if (validUser) {
				// metadata
				rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						PokeStatus.SUCCESS, "Sucessfully Signed In"));
			} else {
				rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						PokeStatus.FAILURE, "Wrong, Username or Password"));
			}

			// payload
			Payload.Builder pb = Payload.newBuilder();
			rb.setBody(pb.build());

			reply = rb.build();
			return reply;
		}// end sign in

		// Get course list
		if (request.getHeader().getTag().equalsIgnoreCase("RequestList")) {

			RequestList.Builder rl = RequestList.newBuilder();
			GetCourse.Builder gc = GetCourse.newBuilder();

			CourseRiak course = new CourseRiak();
			List<Course> courselist = null;

			try {
				courselist = course.getCourseList();
			} catch (RiakException e) {
				e.printStackTrace();
			}

			if (courselist != null) {

				for (Course c : courselist) {

					gc.setCourseName(c.getCourseName());
					gc.setCourseId(c.getCourseId());
					gc.setCourseDescription(c.getDescription());
					rl.addCourseList(gc.build());
				}
				// metadata
				rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						PokeStatus.SUCCESS, "Sucessfully got the course list"));
				Payload.Builder pb = Payload.newBuilder();
				pb.setReqList(rl.build());
				rb.setBody(pb.build());
			}

			else {
				rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						PokeStatus.FAILURE, "Sorry No Course List Found"));
				// payload
				Payload.Builder pb = Payload.newBuilder();
				rb.setBody(pb.build());

			}

			reply = rb.build();
			return reply;
		}// end course list

		// search a course
		if (request.getHeader().getTag().equalsIgnoreCase("SearchCourse")) {
			boolean Iscourse = false;
			CourseRiak course = new CourseRiak();
			try {
				Iscourse = course.isCoursePresent(Integer.toString(request
						.getBody().getGetCourse().getCourseId()));
			} catch (UnresolvedConflictException e1) {
				e1.printStackTrace();
			} catch (RiakRetryFailedException e1) {
				e1.printStackTrace();
			} catch (ConversionException e1) {
				e1.printStackTrace();
			}

			if (Iscourse) {
				// metadata
				rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						PokeStatus.SUCCESS, "Course was successfull searched!"));
				// payload
				Payload.Builder pb = Payload.newBuilder();
				GetCourse.Builder gc = GetCourse.newBuilder();

				gc.setCourseId(request.getBody().getGetCourse().getCourseId());
				try {
					gc.setCourseName(course.getCourse(Integer.toString(request
							.getBody().getGetCourse().getCourseId())).courseName);
					gc.setCourseDescription(course.getCourse(Integer
							.toString(request.getBody().getGetCourse()
									.getCourseId())).description);
				} catch (UnresolvedConflictException e) {
					e.printStackTrace();
				} catch (RiakRetryFailedException e) {
					e.printStackTrace();
				} catch (ConversionException e) {
					e.printStackTrace();
				}

				pb.setGetCourse(gc.build());
				rb.setBody(pb.build());
			} else {
				rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						PokeStatus.FAILURE, "Sorry No Course Found"));
				Payload.Builder pb = Payload.newBuilder();
				rb.setBody(pb.build());
			}

			reply = rb.build();
			return reply;
		}// end search a course

		// voting request
		if (request.getHeader().getTag().equalsIgnoreCase("voting")
				&& request.getBody().hasInitVoting()) {
			rb.setHeader(request.getHeader());
			rb.setBody(request.getBody());
			reply = rb.build();
			return reply;

		}

		reply = ResourceUtil.buildError(request, PokeStatus.NOFOUND,
				"Request not processed as this functionality does not exist");
		return reply;
	}

}
