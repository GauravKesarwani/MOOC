import Mooc
import ClientConnection

# Main
def main():
    	client = ClientConnection
	mooc = Mooc
	#client.request_voting()
	client.get_server_ip_address()
	client.request_ping()

	menu()
	choice= int(input("Enter menu choice:\t"))
	while choice != 3:
		if choice == 1:
    			value1= mooc.signIn()
    			
    			if value1 == 'Sucessfully Signed In':
    				CourseMenu()
    				choice= int(input("Enter menu choice:\t"))
    				while choice != 4:
    					if choice == 1:
    						mooc.getCourseList()
    						break
    					if choice == 2:
    						mooc.searchCourse()
    						break
					if choice == 3:
						client.request_voting()
						break
    				break
    			else:
    				mooc.signIn()
    			
		if choice == 2:
    			value2= mooc.signUp()
    			#print "check", value2
    			if value2 == 'Sucessfully signed up':
    				CourseMenu()
    				choice= int(input("Enter menu choice:\t"))
    				while choice != 4:
    					if choice == 1:
    						mooc.getCourseList()
    						break
    					if choice == 2:
    						mooc.searchCourse()
    						break					
					if choice == 3:
						client.request_voting()
						break
					
    				break
    			else:
 				mooc.signUp()   		
    

def menu():

    #user chooses a number from list
    print("\n Choose a number to continue:\t\n\
    Select 1 to Sign In \n\
    Select 2 to Sign Up\n\
    Select 3 to exit! \n")
    
def CourseMenu():

    #user chooses a number from list
    print("\n Choose a number to continue:\t\n\
    Select 1 to Get the Courses List \n\
    Select 2 to search a course\n\
    Select 3 for voting! \n\
    Select 4 for exit!")
	

if __name__ == '__main__':
    main()
