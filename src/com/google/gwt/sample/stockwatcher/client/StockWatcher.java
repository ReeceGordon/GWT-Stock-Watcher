package com.google.gwt.sample.stockwatcher.client;

import com.google.gwt.sample.stockwatcher.shared.FieldVerifier;

import java.util.ArrayList;
import java.util.Date;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class StockWatcher implements EntryPoint {
	private VerticalPanel mainPanel = new VerticalPanel();
	private FlexTable stocksFlexTable = new FlexTable();
	private HorizontalPanel addPanel = new HorizontalPanel();
	private TextBox newSymbolTextBox = new TextBox();
	private Button addStockButton = new Button("Add");
	private Label lastUpdatedLabel = new Label();
	
	private ArrayList<String> stocks = new ArrayList<String>();
	private static final int REFRESH_INTERVAL = 5000; //ms

	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {
		//Create table for stock data
		stocksFlexTable.setText(0, 0, "Symbol");
		stocksFlexTable.setText(0, 1, "Price");
		stocksFlexTable.setText(0, 2, "Change");
		stocksFlexTable.setText(0, 3, "Remove");
		
		//Style the stocksFlexTable
		stocksFlexTable.setCellPadding(6);
		stocksFlexTable.getRowFormatter().addStyleName(0, "watchListHeader");
		stocksFlexTable.addStyleName("watchList");
		stocksFlexTable.getCellFormatter().addStyleName(0, 1, "watchListNumericColumn");
		stocksFlexTable.getCellFormatter().addStyleName(0, 2, "watchListNumericColumn");
		stocksFlexTable.getCellFormatter().addStyleName(0, 3, "watchListRemoveColumn");
		
		//Assemble add stock panel
		addPanel.add(newSymbolTextBox);
		addPanel.add(addStockButton);
		addPanel.addStyleName("addPanel");
		//Assemble main panel
		mainPanel.add(stocksFlexTable);
		mainPanel.add(addPanel);
		mainPanel.add(lastUpdatedLabel);
		//Associate the main panel with the html host page
		RootPanel.get("stocklist").add(mainPanel);
		//move cursor focus to the input box
		newSymbolTextBox.setFocus(true);
		
		//Setup timer for refresh list automatically
		Timer refreshTimer = new Timer() {
			@Override
			public void run() {
				refreshWatchList();
			}
		};
		refreshTimer.scheduleRepeating(REFRESH_INTERVAL);
		
		//Listen for mouse events on the Add button
		addStockButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				addStock();
			}
		});
		
		//listen for key down event on stock code input
		newSymbolTextBox.addKeyDownHandler(new KeyDownHandler() {
			public void onKeyDown(KeyDownEvent event) {
				if(event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
					addStock();
				}
			}
		});
		
		
	}
	
	private void addStock() {
		final String symbol = newSymbolTextBox.getText().toUpperCase().trim();
		newSymbolTextBox.setFocus(true);
		
		//Stock code must be between 1 and 10 chars that are numbers, letters or dots.
		if(!symbol.matches("^[0-9A-Z\\\\.]{1,10}$")) {
			Window.alert("'" + symbol + "' is not a valid symbol.");
			newSymbolTextBox.selectAll();
			return;
		}
		
		//Don't add stock if its already in the table
		if(stocks.contains(symbol)) {
			Window.alert("'"+ symbol + "' is already in the stock table.");
			newSymbolTextBox.selectAll();
			return;
		}
		
		//Add the stock to the list and ui table
		int row = stocksFlexTable.getRowCount();
		stocks.add(symbol);
		stocksFlexTable.setText(row, 0, symbol);
		
		stocksFlexTable.setWidget(row,  2, new Label()); //Create label widget for every cell in column 2
		
		//Column styles
		stocksFlexTable.getCellFormatter().addStyleName(row, 1,  "watchListNumericColumn");
		stocksFlexTable.getCellFormatter().addStyleName(row, 2, "watchListNumericColumn");
		stocksFlexTable.getCellFormatter().addStyleName(row, 3, "watchListRemoveColumn");
		
		//Add a button to remove the stock from the table
		Button removeStockButton = new Button("x");
		removeStockButton.addStyleDependentName("remove");
		
		removeStockButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				int removedIndex = stocks.indexOf(symbol);
				stocks.remove(removedIndex);
				stocksFlexTable.removeRow(removedIndex + 1);
			}
		});
		stocksFlexTable.setWidget(row,  3,  removeStockButton);
		
		//Get the stock price
		refreshWatchList();
		
		newSymbolTextBox.setText("");
		
		
	}
	
	private void refreshWatchList() {
		final double MAX_PRICE = 100;
		final double MAX_PRICE_CHANGE = 0.02;
		
		StockPrice[] prices = new StockPrice[stocks.size()];
		for(int i = 0; i < stocks.size(); i++) {
			double price = Random.nextDouble() * MAX_PRICE;
			double change = price * MAX_PRICE_CHANGE * (Random.nextDouble() * 2.0 - 1.0);
			
			prices[i] = new StockPrice(stocks.get(i), price, change);
		}
		
		updateTable(prices);
	}
	
	private void updateTable(StockPrice[] prices) {
		for(int i = 0; i < prices.length; i++) {
			updateTable(prices[i]);
		}
		
		//Display timestamp showing last refresh
		DateTimeFormat dateFormat = DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_TIME_MEDIUM);
		lastUpdatedLabel.setText("Last update: " + dateFormat.format(new Date()));
	}
	
	private void updateTable(StockPrice price) {
		//checks if stock is still in stocks table
		if(!stocks.contains(price.getSymbol())) {
			return;
		}
		
		int row = stocks.indexOf(price.getSymbol()) + 1; //Always add +1 to row, 0 the base row is reserved for Titles
		
		String priceText = NumberFormat.getFormat("#,##0.00").format(price.getPrice());
		NumberFormat changeFormat = NumberFormat.getFormat("+#,##0.00;-#,##0.00");
		String changeText = changeFormat.format(price.getChange());
		String changePercentText = changeFormat.format(price.getChangePercent());
		
		//populate the price and change fields with new data
		stocksFlexTable.setText(row,  1,  priceText);
		Label changeWidget = (Label)stocksFlexTable.getWidget(row, 2);
		changeWidget.setText(changeText + " (" + changePercentText + "%)");
		//stocksFlexTable.setText(row, 2, changeText+ " (" + changePercentText + "%)"); //old
		
		String changeStyleName = "noChange";
		if(price.getChangePercent() < -0.1f) {
			changeStyleName = "negativeChange";
		}
		else if(price.getChangePercent() > 0.1f) {
			changeStyleName = "positiveChange";
		}
		
		changeWidget.setStyleName(changeStyleName);
		
		
	}
}
