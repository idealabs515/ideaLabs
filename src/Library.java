//Author : Muhammad Ahmed Shoaib
//Reviewer :Chitty Vaishnav Reddy 
// Moderator: Husayin
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
public class library implements Serializable {
	
	private static final String LIBRARY_FILE = "library.obj";
	private static final int LOAN_LIMIT = 2;
	private static final int LOAN_PERIOD = 2;
	private static final double FINE_PER_DAY = 1.0;
	private static final double MAX_FINES_OWED = 5.0;
	private static final double DAMAGE_FEE = 2.0;
	
	private static Library library; // changed self to library 
	private int bookID; //changed BID to bookID
	private int memberID; //changed MID to memberID
	private int loanID; // changed LID to loadID
	private Date loadDate;
	//Capitalized the model classes below Book, Member and Loan
	private Map<Integer, Book> catalog;
	private Map<Integer, Member> members;
	private Map<Integer, Loan> loans;
	private Map<Integer, Loan> currentLoans;
	private Map<Integer, Book> damagedBooks;
	

	private Library() {
		catalog = new HashMap<>();
		members = new HashMap<>();
		loans = new HashMap<>();
		currentLoans = new HashMap<>();
		damagedBooks = new HashMap<>();
		bookID = 1;
		memberID = 1;		
		loanID = 1;		
	}

	
	public static synchronized Library instance() {	//changed from upper case to lower case	
		if (library == null) {
			Path path = Paths.get(LIBRARY_FILE);			
			if (Files.exists(path)) {	
				try (ObjectInputStream libraryFile = new ObjectInputStream(new FileInputStream(LIBRARY_FILE));) {
			    // changed lof variable with libraryFile
					library = (Library) libraryFile.readObject();
					Calendar.getInstance().setDate(library.loadDate);
					libraryFile.close();
				}
				catch (Exception exception) {
					throw new RuntimeException(exception);
				}
			}
			else {
				library = new Library();
			}
		}
		return library;
	}

	
	public static synchronized void save() { //changed from uppercase to lowercase
		if (library != null) {
			library.loadDate = Calendar.getInstance().Date();
			try (ObjectOutputStream libraryFile = new ObjectOutputStream(new FileOutputStream(LIBRARY_FILE));) {
				libraryFile.writeObject(library);
				libraryFile.flush();
				libraryFile.close();	
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		}
	}

	
	public int bookID() {
		return bookID;
	}
	
	
	public int memberID() {
		return memberID;
	}
	
	//changed nextBID to nextBookID()
	private int nextBookID() {
		return bookID++;
	}

	//changed nextMID to nextMemberID()
	private int nextMemberID() {
		return memberID++;
	}

	//changed nextLID to nextLoanID()
	private int nextLoanID() {
		return LoanID++;
	}

	
	public List<Member> Members() {		
		return new ArrayList<Member>(members.values()); 
	}


	public List<Book> Books() {		
		return new ArrayList<Book>(catalog.values()); 
	}


	public List<Loan> CurrentLoans() {
		return new ArrayList<Loan>(currentLoans.values());
	}

	//changed Add_Mem to addMember()
	public Member addMember(String lastName, String firstName, String email, int phoneNo) {		
		Member member = new Member(lastName, firstName, email, phoneNo, nextMemberID());
		members.put(member.getId(), member);		
		return member;
	}

	//changed Add_book to addBook()
	public Book addBook()(String author, String title, String category) {	 // changed a to author, t to title c to category	
		Book book = new Book(author, title, category, nextBookID()); //changed b to book
		catalog.put(Book.ID(), book);		
		return book;
	}

	public Member getMember(int memberId) {
		if (members.containsKey(memberId)) {
			return members.get(memberId);
		}
		return null;
	}

	// changed method book to getBook()
	public Book getBook(int bookId) {
		if (catalog.containsKey(bookId)) {
			return catalog.get(bookId);
		}
		return null;
	}

	
	public int loanLimit() {
		return LOAN_LIMIT;
	}

	public boolean memberCanBorrow(Member member) {		
		if (member.getNumberOfCurrentLoans() == LOAN_LIMIT ) {
			return false;
		}		
		if (member.getFinesOwed() >= MAX_FINES_OWED) {
			return false;
		}
				
		for (Loan loan : member.getLoans()) {
			if (loan.isOverDue()) {
				return false;
			}
		}
			
		return true;
	}

	// changed method loansRemainingForMember to currentMemberLoans
	public int currentMemberLoans(Member member) {		
		return LOAN_LIMIT - member.getNumberOfCurrentLoans();
	}

	
	public Loan issueLoan(Book book, Member member) {
		Date dueDate = Calendar.getInstance().getDueDate(LOAN_PERIOD);
		Loan loan = new Loan(nextLoanID(), book, member, dueDate);
		member.takeOutLoan(loan);
		book.Borrow();
		loans.put(loan.getId(), loan);
		currentLoans.put(book.ID(), loan);
		return loan;
	}
	
	
	public Loan getLoanByBookId(int bookId) {
		if (currentLoans.containsKey(bookId)) {
			return currentLoans.get(bookId);
		}
		return null;
	}

	// changed calculateOverDueFine to calculateFine()
	public double calculateFine(Loan loan) {
		if (loan.isOverDue()) {
			long daysOverDue = Calendar.getInstance().getDaysDifference(loan.getDueDate());
			double fine = daysOverDue * FINE_PER_DAY;
			return fine;
		}
		return 0.0;		
	}


	public void dischargeLoan(Loan currentLoan, boolean isDamaged) {
		Member member = currentLoan.Member();
		Book book  = currentLoan.Book();
		
		double overDueFine = calculateOverDueFine(currentLoan);
		member.addFine(overDueFine);	
		
		member.dischargeLoan(currentLoan);
		book.Return(isDamaged);
		if (isDamaged) {
			member.addFine(DAMAGE_FEE);
			damagedBooks.put(book.ID(), book);
		}
		currentLoan.Loan();
		currentLoans.remove(book.ID());
	}


	public void checkCurrentLoans() {
		for (Loan loan : currentLoans.values()) {
			loan.checkOverDue();
		}		
	}


	public void repairBook(Book currentBook) {
		if (damagedBooks.containsKey(currentBook.ID())) {
			currentBook.Repair();
			damagedBooks.remove(currentBook.ID());
		}
		else {
			throw new RuntimeException("Library: repairBook: book is not damaged");
		}
		
	}
	
	
}
