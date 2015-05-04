package mooc.storage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.basho.riak.client.RiakRetryFailedException;
import com.basho.riak.client.cap.UnresolvedConflictException;
import com.basho.riak.client.convert.ConversionException;

//the class is a test class for checking the database functionalities and inserting data in the database
public class MainTest {

	public static void main(String[] args) throws UnresolvedConflictException,
			RiakRetryFailedException, ConversionException {

		User user = new User();
		user.setCredentials("swap", "password");
		boolean val = user.signIn("swap", "password");
		System.out.println(val);

		List<Course> crs = new ArrayList<Course>();

		CourseRiak cr = new CourseRiak();
		cr.setCourses(1, "CMPE275", "By John Gash");
		cr.setCourses(2, "CMPE277", "By Chandra");
		cr.setCourses(3, "CMPE274", "By Yu");
		try {
			crs = cr.getCourseList();

			Iterator<Course> c2 = crs.iterator();
			while (c2.hasNext())
				System.out.println(c2.next().courseName);

		} catch (Exception e) {
			e.printStackTrace();

		}

	}

}
