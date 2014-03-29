package me.valour.bowls;

import android.app.Activity;
import android.app.Fragment;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

public class BillFragment extends Fragment implements Bill.BillChangesAgent, LineItemAdapter.LineItemAgent{

	private Bill bill;
	private TextView amountSubtotal;
	private TextView percentTax;
	private TextView amountTax;
	private TextView percentTip;
	private TextView amountTip;
	private TextView amountTotal;
	private TextView newLineItem;
	
	private ListView listView;
	private LineItemAdapter adapter;
	
	private LineItemListener newLineItemSpy; 
	
	public BillFragment(){}
	
	private int selectedLI = -1;
	private View prevView = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
		View view =  inflater.inflate(R.layout.fragment_bill, container, false);
		listView = (ListView) view.findViewById(R.id.lineItemsList);
		adapter = new LineItemAdapter(getActivity(), bill.lineItems, this);
		adapter.setNotifyOnChange(true);
		listView.setAdapter(adapter);
		
		listView.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position,
					long id) {
				if(selectedLI==position){ //go into edit mode
					newLineItemSpy.EditLineItem();
				} else {
					if(hasSelectedLineItem()){
						deselectLineItem();
					}
					selectLineItem(view, position);
					//TODO: re-select the chosen bowls
				}
			}
			
		});
		
		amountSubtotal = (TextView) view.findViewById(R.id.subtotal_amount);
		amountTotal = (TextView) view.findViewById(R.id.total_amount);
		percentTax = (TextView) view.findViewById(R.id.tax_percent);
		amountTax = (TextView) view.findViewById(R.id.tax_amount);
		percentTip = (TextView) view.findViewById(R.id.tip_percent);
		amountTip = (TextView) view.findViewById(R.id.tip_amount);
		
		newLineItem = (TextView) view.findViewById(R.id.newLineItem);
		newLineItem.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(hasSelectedLineItem()){
					deselectLineItem();
				}
				newLineItemSpy.OnNewLineItem();
			}
		});
		
		return view;
    }

	/* (non-Javadoc)
	 * @see android.app.Fragment#onAttach(android.app.Activity)
	 */
	@Override
	public void onAttach(Activity activity) {
		// TODO Auto-generated method stub
		super.onAttach(activity);
		try {
			bill = ((TableActivity)activity).getBill();
			newLineItemSpy = (LineItemListener) activity;
			bill.attachAgent(this);
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnFragmentInteractionListener");
		}
	}

	/* (non-Javadoc)
	 * @see android.app.Fragment#onDetach()
	 */
	@Override
	public void onDetach() {
		// TODO Auto-generated method stub
		super.onDetach();
	}
	
	public void selectLineItem(View view, int position){
		view.findViewById(R.id.tap2edit).setVisibility(View.VISIBLE);
		prevView = view;
		selectedLI = position;
		newLineItemSpy.SelectLineItem(position);
	}
	
	public void deselectLineItem(){
		if(prevView!=null){
			prevView.findViewById(R.id.tap2edit).setVisibility(View.INVISIBLE);
		}
		prevView = null;
		selectedLI = -1;
	}
	
	public boolean hasSelectedLineItem(){
		return prevView!=null && selectedLI>-1;
	}
	/*
	public void updateSubtotal(){
		double sum = 0.0;
		for(LineItem li: bill.lineItems){
			sum += li.getPrice();
		}
		setSubtotal(sum);
	}
	*/
	
	private void setSubtotal(double t){
		amountSubtotal.setText(getString(R.string.x_dollars, t));
	}
	
	private void setTotal(double t){
		amountTotal.setText(getString(R.string.x_dollars, t));
	}
	
	private void setTaxPercent(double t){
		percentTax.setText(getString(R.string.x_percent, (t*100)));
	}
	
	private void setTipPercent(double t){
		percentTip.setText(getString(R.string.x_percent, (t*100)));
	}
	
	private void setTaxAmount(double t){
		amountTax.setText(getString(R.string.x_dollars, t));
	}
	
	private void setTipAmount(double t){
		amountTip.setText(getString(R.string.x_dollars, t));
	}
	
	public void clearSummary(){
		setSubtotal(0.0);
		setTotal(0.0);
		setTaxPercent(bill.getTax());
		setTaxAmount(0.0);
		setTipPercent(bill.getTip());
		setTipAmount(0.0);
	}
	
	public void adjustForSplitEqually(){
		listView.setVisibility(View.GONE);
		newLineItem.setVisibility(View.GONE);
	}
	
	public void updatedList(){
		adapter.notifyDataSetChanged();
	}
	
	public interface LineItemListener{
		public void OnNewLineItem();
		public void SelectLineItem(int position);
		public void EditLineItem();
		public void updateBowlsPrice();
	}

	@Override
	public void subtotalChanged() {
		setSubtotal(bill.getSubtotal());
		updateTotal();
	}

	@Override
	public void taxChanged(boolean rateChanged) {
		if(rateChanged){
			setTaxPercent(bill.getTax());
		}
		setTaxAmount(bill.calculateTax());
		updateTotal();
	}

	@Override
	public void tipChanged(boolean rateChanged) {
		if(rateChanged){
			setTipPercent(bill.getTip());
		}
		setTipAmount(bill.calculateTip());
		updateTotal();
	}

	@Override
	public void updateTotal() {
		double total = bill.getSubtotal() + bill.calculateTax() + bill.calculateTip();
		setTotal(total);
	}
/*
	@Override
	public void taxCleared() {
		setTaxPercent(0.0);
		setTaxAmount(0.0);
		setTotal(bill.getSubtotal());
	}

	@Override
	public void tipCleared() {
		setTipPercent(0.0);
		setTipAmount(0.0);
		setTotal(bill.getSubtotal());
	}
	*/

	@Override
	public void deleteLI(int position, LineItem li) {
		Log.d("vars", "ready to delete "+li.toString());
		bill.rmLineItem(position);
		newLineItemSpy.updateBowlsPrice();
		adapter.notifyDataSetChanged();
	}
}
