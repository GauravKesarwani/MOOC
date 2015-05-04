package mooc.storage;

/* 
 * this class performs the function of setting, retrieiving course objects in database
 * DB Connection
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.RiakFactory;
import com.basho.riak.client.RiakRetryFailedException;
import com.basho.riak.client.bucket.Bucket;
import com.basho.riak.client.cap.UnresolvedConflictException;
import com.basho.riak.client.convert.ConversionException;

public class CourseRiak {

	IRiakClient client;
	Bucket courseBucket;

	public CourseRiak() {
		// Create client connection and initialize the bucket
		try {
			client = RiakFactory.httpClient("http://192.168.0.79:8098/riak");

			courseBucket = client.fetchBucket("test1").execute();
		} catch (Exception e) {
			e.getMessage();
		}
	}

	// Method to insert the course Id, Course name and Course Description with
	// Course Id being the key
	public void setCourses(int courseId, String courseName, String description) {
		Course course = new Course();
		course.courseId = courseId;
		course.courseName = courseName;
		course.description = description;
		String strId = Integer.toString(course.courseId);

		try {
			courseBucket.store(strId, course).execute();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Method to check whether any course exists or not.
	public boolean isAnyCourse() throws RiakException {

		Iterator<String> i1 = courseBucket.keys().iterator();
		if (i1.hasNext())
			return true;
		else
			return false;
	}

	// Method to return the List of Courses
	public List<Course> getCourseList() throws RiakException {

		Iterator<String> i1 = courseBucket.keys().iterator();
		ArrayList<Course> list = new ArrayList<Course>();

		while (i1.hasNext()) {

			String str = i1.next();
			Course crs = courseBucket.fetch(str, Course.class).execute();
			list.add(crs);

		}

		return list;
	}

	// Method to return the course details for the given Course ID
	public Course getCourse(String courseId)
			throws UnresolvedConflictException, RiakRetryFailedException,
			ConversionException {
		if (courseBucket.fetch(courseId).execute() != null) {

			Course course = courseBucket.fetch(courseId, Course.class)
					.execute();
			return course;
		} else
			return null;
	}

	// Method to check if a particular course exists in the Database or not.
	public boolean isCoursePresent(String courseId)
			throws UnresolvedConflictException, RiakRetryFailedException,
			ConversionException {
		if (courseBucket.fetch(courseId).execute() != null)
			return true;
		else
			return false;
	}

}
