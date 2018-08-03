import java.util.ArrayList;
import java.util.List;

public class BorrowBookControl {
	//enum definition has been carried to top of the class
	private enum CONTROL_STATE { INITIALISED, READY, RESTRICTED, SCANNING, IDENTIFIED, FINALISING, COMPLETED, CANCELLED };
	
	private BorrowBookUI borrowBookUI; //ui renamed to borrowBookUI
	
	private Library library; //L renemad to library
	private Member member; //M renamed to member
	private CONTROL_STATE controlState; //state renamed to controlState
	
	private List<Book> pendingBookList; //PENDING renamed to pendingBookList
	private List<Loan> completedLoanList; //COMPLETED renamed to completedLoanList
	private Book book; //B renamed to book
	
	public BorrowBookControl() {
		this.library = library.INSTANCE();
		controlState = CONTROL_STATE.INITIALISED;
	}
	

	public void setBorrowBookUI(BorrowBookUI borrowBookUI) {
		if (!controlState.equals(CONTROL_STATE.INITIALISED)) 
			throw new RuntimeException("BorrowBookControl: cannot call setBorrowBookUI except in INITIALISED state");
			
		this.borrowBookUI = borrowBookUI;
		borrowBookUI.setState(BorrowBookUI.UI_STATE.READY);
		controlState = CONTROL_STATE.READY;		
	}

		
	public void cardSwiped(int memberId) {
		if (!controlState.equals(CONTROL_STATE.READY)) 
			throw new RuntimeException("BorrowBookControl: cannot call cardSwiped except in READY state");
			
		member = library.getMember(memberId);
		if (member == null) {
			borrowBookUI.display("Invalid memberId");
			return;
		}
		if (library.memberCanBorrow(member)) {
			pendingBookList = new ArrayList<>();
			borrowBookUI.setState(BorrowBookUI.UI_STATE.SCANNING);
			controlState = CONTROL_STATE.SCANNING; }
		else 
		{
			borrowBookUI.display("Member cannot borrow at this time");
			borrowBookUI.setState(BorrowBookUI.UI_STATE.RESTRICTED); }}
	
	
	public void bookScanned(int bookId) {
		book = null;
		if (!controlState.equals(CONTROL_STATE.SCANNING)) {
			throw new RuntimeException("BorrowBookControl: cannot call bookScanned except in SCANNING state");
		}	
		book = library.Book(bookId);
		if (book == null) {
			borrowBookUI.display("Invalid bookId");
			return;
		}
		if (!book.Available()) {
			borrowBookUI.display("Book cannot be borrowed");
			return;
		}
		pendingBookList.add(book);
		for (Book tempBook : pendingBookList) {
			borrowBookUI.display(tempBook.toString());
		}
		if (library.loansRemainingForMember(member) - pendingBookList.size() == 0) {
			borrowBookUI.display("Loan limit reached");
			Complete();
		}
	}
	
	
	public void complete() {
		if (pendingBookList.size() == 0) {
			cancel();
		}
		else {
			borrowBookUI.display("\nFinal Borrowing List");
			for (Book tempBook : pendingBookList) {
				borrowBookUI.display(tempBook.toString());
			}
			completedLoanList = new ArrayList<loan>();
			borrowBookUI.setState(BorrowBookUI.UI_STATE.FINALISING);
			controlState = CONTROL_STATE.FINALISING;
		}
	}


	public void commitLoans() {
		if (!controlState.equals(CONTROL_STATE.FINALISING)) {
			throw new RuntimeException("BorrowBookControl: cannot call commitLoans except in FINALISING state");
		}	
		for (Book tempBook : pendingBookList) {
			Loan loan = library.issueLoan(tempBook, member);
			completedLoanList.add(loan);			
		}
		borrowBookUI.display("Completed Loan Slip");
		for (Loan loan : completedLoanList) {
			borrowBookUI.display(loan.toString());
		}
		borrowBookUI.setState(BorrowBookUI.UI_STATE.COMPLETED);
		controlState = CONTROL_STATE.COMPLETED;
	}

	
	public void cancel() {
		borrowBookUI.setState(BorrowBookUI.UI_STATE.CANCELLED);
		controlState = CONTROL_STATE.CANCELLED;
	}
	
	
}
