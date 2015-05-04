package mooc.storage;

import java.util.ArrayList;
import java.util.Iterator;

import com.basho.riak.client.*;
import com.basho.riak.client.bucket.*;
import com.basho.riak.client.cap.UnresolvedConflictException;
import com.basho.riak.client.convert.ConversionException;

//User class, defines the user
public class User {

	IRiakClient client;
	Bucket b;

	public User() {

		// Create client connection and initialize the bucket
		try {
			client = RiakFactory.httpClient("http://192.168.0.79:8098/riak");
			b = client.fetchBucket("test").execute();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// Set the User Credentials while Sign Up
	public void setCredentials(String userName, String password) {

		try {
			b.store(userName, password).execute();
		} catch (Exception e) {
			e.getMessage();
		}
	}

	// Method to get the password for a particular User
	private String getPassword(String uName)
			throws UnresolvedConflictException, RiakRetryFailedException,
			ConversionException {

		IRiakObject riakObject;

		riakObject = b.fetch(uName).execute();
		String password = riakObject.getValueAsString();
		return password;
	}

	// Method to check whether a user exist or not
	public boolean isuserName(String userName)
			throws UnresolvedConflictException, RiakRetryFailedException,
			ConversionException {

		if (b.fetch(userName).execute() == null)
			return false;
		else
			return true;
	}

	// Method to sign in the user. The method will return true if correct
	// combination of user and password is enetered
	public boolean signIn(String userName, String password)
			throws UnresolvedConflictException, RiakRetryFailedException,
			ConversionException {
		if (!isuserName(userName))
			return false;

		if (getPassword(userName).equals(password))
			return true;
		else
			return false;
	}

	// An array List to return the list of users
	public ArrayList<String> getUserList() throws RiakException {
		Iterator<String> i1 = b.keys().iterator();
		ArrayList<String> list = new ArrayList<String>();

		while (i1.hasNext()) {

			list.add(i1.next());

		}

		return list;
	}

}
