import ClientConnection

def signUp():
	obj = ClientConnection
	fullName= raw_input(" \n Welcome to SIGNIN UP !! \n \n Please enter your Full Name :")
	username= raw_input("\n Please enter the username/emailID :")
    	password= raw_input("Please enter the password :") 
	return obj.request_sign_up(fullName, username, password)


def signIn():
	obj = ClientConnection
	username= raw_input(" \n Welcome to SIGN IN !! \n \n Please enter your Username:")
    	password= raw_input("Please enter your password:") 
	return obj.request_sign_in(username, password)

def getCourseList():
	obj = ClientConnection
	obj.request_course_list()

def searchCourse():
	obj = ClientConnection
	courseId= int(raw_input("\n Please enter the course ID of the course you want to search:"))
    	obj.request_search_course(courseId)