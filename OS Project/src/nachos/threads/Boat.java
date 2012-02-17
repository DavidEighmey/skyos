package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
    static BoatGrader bg;
 
    	
    
    private static Lock lock; 	//declare lock
    private static int childrenOnOahu; //total children on Oahu
    private static int childrenOnMolokai; // total children on Mookai
    
    private static int adultsOnOahu; // total adults on Oahu
    
    private static int childrenOnBoat;
    private static int adultsOnBoat;
    
    
    
    
    
    public static void selfTest()
    {
	BoatGrader b = new BoatGrader();
	
	System.out.println("\n ***Testing Boats with only 2 children***");
	begin(0, 2, b);

//	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
//  	begin(1, 2, b);

//  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
//  	begin(3, 3, b);
    }

    public static void begin( int adults, int children, BoatGrader b )
    {
	// Store the externally generated autograder in a class
	// variable to be accessible by children.
	bg = b;

	// Instantiate global variables here
	lock = new Lock();
	childrenOnOahu = 0; 	//starts everything off at 0
	childrenOnMolokai = 0;
	adultsOnOahu = 0;
	childrenOnBoat = 0;
	adultsOnBoat = 0;
	
	
	
	// Create threads here. See section 3.4 of the Nachos for Java
	// Walkthrough linked from the projects page.
	lock.acquire();

	Runnable r = new Runnable() {
	    public void run() {
                SampleItinerary();
            }
        };
        KThread t = new KThread(r);
        t.setName("Sample Boat Thread");
        t.fork();

    }

    static void AdultItinerary()
    {
	/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
	*/
    	lock.acquire(); //get the lock 
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	lock.release();//releasing the lock
    }

    static void ChildItinerary()
    {
    	lock.acquire(); // get the lock 
    	
    	
    	
    	
    	
    	
    	
    	
    	lock.release(); // release the lock
    }

    static void SampleItinerary()
    {
	// Please note that this isn't a valid solution (you can't fit
	// all of them on the boat). Please also note that you may not
	// have a single thread calculate a solution and then just play
	// it back at the autograder -- you will be caught.
	System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
	bg.AdultRowToMolokai();
	bg.ChildRideToMolokai();
	bg.AdultRideToMolokai();
	bg.ChildRideToMolokai();
    }
    
}
