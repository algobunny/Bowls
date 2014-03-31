package me.valour.bowls;

import java.util.List;

import me.valour.bowls.enums.Action;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class TableActivity extends Activity implements
		BowlsGroup.BowlsGroupAgent, 
		TableFragment.ButtonAgent,
		BillFragment.BillFragmentAgent, 
		NumberPadFragment.CloseNumpadListener {

	private int bowlsCount;
	private Bill bill;
	private FragmentManager fm;

	private TableFragment tableFragment;
	private NumberPadFragment numFragment;
	private BillFragment billFragment;
	
	public boolean splitEqually;
	private Action action;
	
	private SharedPreferences sp;
	
	private LineItem selectedLineItem = null;
	private BowlView deleteBowlQueue = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		splitEqually = intent.getBooleanExtra("splitEqually", true);
		
		sp = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

		bowlsCount = Kitchen.minBowls;
		action = Action.ITEM_PRICE;
		bill = new Bill(getTax(), getTip());

		setContentView(R.layout.activity_table);

		fm = getFragmentManager();
		tableFragment = (TableFragment) fm.findFragmentById(R.id.tableFragment);
		numFragment = new NumberPadFragment();
		
		bill.addUniqueUsers(tableFragment.bowlsGroup.getBowlUsers());
		
		billFragment = (BillFragment) fm.findFragmentById(R.id.billFragment);
		billFragment.clearSummary();
		
		if (splitEqually) {
			initSplitEqually();
		} else {
			initSplitLineItems();
		} 
		
	}

	public void applyTax() {
		bill.applyTax();
		updateBowlsPrice();
	}

	public double getTax() {
		if (sp == null) {
			return 0.0;
		} else {

			String stringTax = sp.getString("default_tax", getString(R.string.tax_percent));
			return Double.parseDouble(stringTax) / 100.0;
		}
	}

	public void setTax(String tax, boolean save) {
		double percent = Double.parseDouble(tax) / 100;
		bill.setTax(percent);
		if (sp != null && save) {
			sp.edit().putString("default_tax", tax).commit();
		}
	}

	public void applyTip() {
		bill.applyTip();
		updateBowlsPrice();
	}

	public double getTip() {
		if (sp == null) {
			return 0.0;
		} else {
			String stringTip = sp.getString("default_tip", getString(R.string.tip_percent));
			return Double.parseDouble(stringTip) / 100.0;
		}
	}

	public void setTip(String tip, boolean save) {
		double percent = Double.parseDouble(tip) / 100;
		bill.setTip(percent);

		if (sp != null && save) {
			sp.edit().putString("default_tip", tip).commit();
		}
	}

	private void initSplitEqually() {
		tableFragment.setQuestionText(R.string.q_enter_subtotal);
		
		billFragment.adjustForSplitEqually();
		action = Action.ENTER_SUBTOTAL;
		onNewLineItem();
	}

	private void initSplitLineItems() {
		tableFragment.setQuestionText(R.string.q_enter_first_li);
	}

	private void clearCenter() {
		action = Action.NONE;
		tableFragment.setQuestionText(null);
		tableFragment.showNoButton(false);
		tableFragment.showOkButton(false);
	}

	private void completePercentChange() {
		numFragment.clearField();
		numFragment.setAsDollarMode();
		numFragment.highlightTextField(false);
		clearCenter();
	}
	
	private void completeConfirmDelete(){
		tableFragment.setNoButtonText(R.string.no);
		if(splitEqually){
			tableFragment.setQuestionText(null);
			tableFragment.showNoButton(false);
			tableFragment.showOkButton(false);
		} else {
			tableFragment.setQuestionText(R.string.q_enter_next_li);
			action = Action.ITEM_PRICE;
		}
	}

	private void registerItemPrice() {
		double price = numFragment.getNumberValue();
		LineItem li = bill.addLineItem(price);
		if (splitEqually) {
			bill.divideEqually(li);
			clearCenter();
		} else {
			prepareForSelectingBowls(li);
			billFragment.updatedList();
		}
		updateBowlsPrice();
	}
	
	private void updateItemPrice(){
		double price = numFragment.getNumberValue();
		bill.updateLineItemPrice(selectedLineItem, price);
		if(splitEqually){
			bill.redivideEqually();
			clearCenter();
		} else {
			bill.redivideAmongst(selectedLineItem);
			billFragment.updatedList();
		}
		updateBowlsPrice();
	}

	private void prepareForSelectingBowls(LineItem li) {
		tableFragment.setQuestionText(R.string.q_select_bowls);
		tableFragment.showOkButton(true);
		if(selectedLineItem!=li){
			selectedLineItem = li;
		}
		tableFragment.bowlsGroup.readyBowlSelect();
		action = Action.SELECT_BOWLS;
	}

	private void handleSelectedBowls() {
		if (selectedLineItem != null) {
			List<User> consumers = tableFragment.bowlsGroup.getSelectedUsers();
			if(consumers.isEmpty()){
				tableFragment.setQuestionText(R.string.q_min_select);
				return;
			}
			bill.divideAmongst(selectedLineItem, consumers);

			tableFragment.bowlsGroup.stopBowlSelect();

			// move to being ready for next Item
			tableFragment.setQuestionText(null);
			tableFragment.showOkButton(false);
			action = Action.ITEM_PRICE;
			selectedLineItem = null;
		}
		updateBowlsPrice();
	}

	@Override
	public void OnOkButtonPress() {
		switch (action) {
		case ITEM_PRICE:
			registerItemPrice();
			break;
		case SELECT_BOWLS:
			handleSelectedBowls();
			break;
		case CONFIRM_TIP:
			/* apply default tip */
			applyTip();
			clearCenter();
			break;
		case SET_TIP:
			/* apply tip at new rate */
			setTip(numFragment.getStringValue(), false);
			applyTip();
			completePercentChange();
			break;
		case CONFIRM_TAX:
			applyTax();
			clearCenter();
			break;
		case SET_TAX:
			setTax(numFragment.getStringValue(), false);
			applyTax();
			completePercentChange();
		case CONFIRM_DELETE:
			if(deleteBowlQueue!=null){
				removeUserDo(deleteBowlQueue.user);
			}
			break;
		default:
			break;
		}
	}

	@Override
	public void OnNoButtonPress() {
		switch (action) {
		case CONFIRM_TAX:
			openNumberPadForPercentChange(bill.getTax());
			tableFragment.showNoButton(false);
			tableFragment.showOkButton(false);
			action = Action.SET_TAX;
			break;

		case CONFIRM_TIP:
			openNumberPadForPercentChange(bill.getTip());
			tableFragment.showNoButton(false);
			tableFragment.showOkButton(false);
			action = Action.SET_TIP;
			break;
		case CONFIRM_DELETE:
			deleteBowlQueue = null;
			completeConfirmDelete();
			break;
		default:
			break;
		}

	}

	@Override
	public void onTipButtonPress(View v) {
		Button btn = (Button) v;
		String txt = btn.getText().toString();
		if (txt.contains("+")) {
			tableFragment.askToAppy("tip", bill.getTip());
			action = Action.CONFIRM_TIP;
			txt = txt.replaceFirst("\\+", "\\-");
		} else {
			bill.clearTip();
			updateBowlsPrice();
			txt = txt.replaceFirst("\\-", "\\+");
		}
		btn.setText(txt);
	}

	@Override
	public void onTaxButtonPress(View v) {
		Button btn = (Button) v;
		String txt = btn.getText().toString();
		if (txt.contains("+")) {
			tableFragment.askToAppy("tax", bill.getTax());
			action = Action.CONFIRM_TAX;
			txt = txt.replaceFirst("\\+", "\\-");
		} else {
			bill.clearTax();
			updateBowlsPrice();
			txt = txt.replaceFirst("\\-", "\\+");
		}
		btn.setText(txt);
	}

	@Override
	public void addUser(User user) {
		if (bowlsCount == Kitchen.maxBowls) {

		} else {
			bowlsCount++;
			bill.addUser(user);
			if (splitEqually) {
				bill.redivideEqually();
				bill.reapplyTax();
				bill.reapplyTip();
				updateBowlsPrice();
			}
		}	
	}

	@Override
	public boolean removeUserConfirm(BowlView bv) {
		User user = bv.user;
		if(bill.allowRmUser(user)){
			removeUserDo(user);
			return true;
		} else {
			tableFragment.setQuestionText(R.string.q_user_has_balance);
			tableFragment.setNoButtonText(R.string.cancel);
			tableFragment.showOkButton(true);
			action = Action.CONFIRM_DELETE;
			deleteBowlQueue = bv;
			return false;
		}
	}

	@Override
	public void removeUserDo(User user) {
		bill.clearUserItems(user);
		bill.rmRow(user);
		if(deleteBowlQueue!=null){
			tableFragment.bowlsGroup.removeBowl(deleteBowlQueue);
			deleteBowlQueue = null;
		}
		if(action==Action.CONFIRM_DELETE){
			completeConfirmDelete();
		}
		bill.reapplyTax();
		bill.reapplyTip();
		updateBowlsPrice();
	}
	
	public Bill getBill(){
		return bill;
	}
	
	/*
	 * BillFragmentAgent methods BEGIN
	 */

	@Override
	public void onNewLineItem() {
		Log.d("vars", "new line item");
		numFragment.setArguments(new Bundle());
		FragmentTransaction ft = fm.beginTransaction();
		ft.setCustomAnimations(R.animator.to_nw, R.animator.to_se);
		ft.replace(R.id.rightContainer, numFragment);
		ft.addToBackStack("bill");
		ft.commit();
	}
	

	@Override
	public void selectLineItem(int position) {
		selectedLineItem = bill.lineItems.get(position);
		prepareForSelectingBowls(selectedLineItem);
		tableFragment.bowlsGroup.manualSelect(bill.listUsers(selectedLineItem));
	}
	
	@Override
	public void editLineItem() {
		if(selectedLineItem==null){
			return;
		}
		double price = selectedLineItem.getPrice();
		
		Bundle bundle = new Bundle();
		bundle.putDouble("numberValue", price);
		bundle.putBoolean("percentMode",false);
		numFragment.setArguments(bundle);
		
		FragmentTransaction ft = fm.beginTransaction();
		ft.setCustomAnimations(R.animator.to_nw, R.animator.to_se);
		ft.replace(R.id.rightContainer, numFragment);
		ft.addToBackStack(null);
		ft.commit();
		
		tableFragment.bowlsGroup.stopBowlSelect();
	}
	
	@Override
	public void updateBowlsPrice() {
		tableFragment.bowlsGroup.refreshBowls();
	}

	@Override
	public void editSubtotal() {
		action = Action.EDIT_SUBTOTAL;
		selectedLineItem = bill.lineItems.get(0); 
		editLineItem();
	}
	
	/*
	 * BillFragmentAgent methods END
	 */

	
	public void openNumberPadForPercentChange(double percent){
		Bundle bundle = new Bundle();
		bundle.putDouble("numberValue", percent*100);
		bundle.putBoolean("percentMode",true);
		numFragment.setArguments(bundle);
		
		FragmentTransaction ft = fm.beginTransaction();
		ft.setCustomAnimations(R.animator.to_nw, R.animator.to_se);
		ft.replace(R.id.rightContainer, numFragment);
		ft.addToBackStack(null);
		ft.commit();
	}
	
	public void closeNumberPad(){
		fm.popBackStack();
	}

	@Override
	public void numPadClose(boolean isEditMode) {
		if(isEditMode){
			switch(action){
			case SET_TAX:
				setTax(numFragment.getStringValue(), false);
				applyTax();
				clearCenter();
				break;
			case SET_TIP:
				setTip(numFragment.getStringValue(), false);
				applyTip();
				clearCenter();
				break;
			case EDIT_SUBTOTAL:
				updateItemPrice();
				break;
			default:
				updateItemPrice();
				prepareForSelectingBowls(selectedLineItem);
				tableFragment.bowlsGroup.manualSelect(bill.listUsers(selectedLineItem));
				break;
			}
		} else {
			registerItemPrice();
		}
		closeNumberPad();
	}

}
