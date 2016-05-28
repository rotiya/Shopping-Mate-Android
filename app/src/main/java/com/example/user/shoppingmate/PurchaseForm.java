package com.example.user.shoppingmate;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;

import Adapter.ItemBaseAdapter;
import DAO.ItemDAO;
import DAO.OrderDAO;
import DTO.ItemDTO;
import DTO.OrderDTO;
import Database.Server;
import Security.LoginTrack;

public class PurchaseForm extends AppCompatActivity {
    Button btnAddToCart, btnCheckOut;
    ListView lstItem;
    TextView txtTotal;

    ArrayList<ItemDTO> itemList = null;
    HashMap<Integer, ItemDTO> itemHash = null;

    ItemBaseAdapter adapter = null;

    //Shopping cart total
    double total = 0.00;

    //Add item to cart
    public void addItemToCart(){
        if(LoginTrack.isValid()){
            Intent intent = new Intent("com.google.zxing.client.android.SCAN");
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
            startActivityForResult(intent, 0);
        }else{
            Intent intent = new Intent(PurchaseForm.this , Login.class);
            startActivity(intent);
        }
    }

    //Checking out function
    public void checkOut(){
        if(LoginTrack.isValid()){
            View view = (LayoutInflater.from(ContextObject.GetContext())).inflate(R.layout.activity_input_pin, null);

            //If shopping cart is empty
            if(itemList.size() == 0){
                Toast.makeText(PurchaseForm.this, "No items in the cart", Toast.LENGTH_SHORT).show();
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(PurchaseForm.this);
            builder.setTitle("Confirm");
            builder.setView(view);

            final EditText txtPin = (EditText) view.findViewById(R.id.txtInputPinPin);
            builder.setCancelable(true);
            builder.setPositiveButton("Enter", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (txtPin.getText().toString().equals("")) {
                        //If pin is empty
                        Toast.makeText(PurchaseForm.this, "Please enter the pin", Toast.LENGTH_SHORT).show();
                    } else {
                        OrderDTO orderDTO = new OrderDTO(LoginTrack.getCurrentUser().getId(), itemList, txtPin.getText().toString());
                        OrderDAO orderDAO = new OrderDAO();
                        orderDAO.insert(new Server.AsyncWriteCompleteListener() {
                            @Override
                            public void onTaskComplete(String result) {
                                Toast.makeText(PurchaseForm.this, result, Toast.LENGTH_SHORT).show();
                            }
                        },orderDTO);
                    }
                }
            });
            Dialog dialog = builder.create();
            dialog.show();
        }
    }

    //Delete item from list
    public void deleteItem(int position){
        //Update total
        updateTotal(-1*itemList.get(position).getPrice());

        //remove item from the list
        itemList.remove(position);

        //Notify list adapter
        adapter.notifyDataSetChanged();
    }

    //Clear list
    public void clearList(){
        //Clear the item list
        itemList.clear();

        //Notify adapter
        adapter.notifyDataSetChanged();

        //Set total to 0
        txtTotal.setText("Total : Rs. ");
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //Analyze QR result
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                try {
                    //Load item description from server
                    new ItemDAO().read(new Server.AsyncReadCompleteListener() {
                        @Override
                        public void onTaskComplete(JSONArray jsonArray) {
                            //Set parameters to the data transfer item
                            ItemDTO itemDTO = new ItemDTO();
                            try {
                                itemDTO.setId(jsonArray.getJSONObject(0).getInt("id"));
                                itemDTO.setProductId(jsonArray.getJSONObject(0).getInt("productId"));
                                itemDTO.setName(jsonArray.getJSONObject(0).getString("name"));
                                itemDTO.setDescription(jsonArray.getJSONObject(0).getString("description"));
                                itemDTO.setPrice(jsonArray.getJSONObject(0).getDouble("price"));
                                itemDTO.setType(jsonArray.getJSONObject(0).getString("type"));
                                itemDTO.setMaxQuantity(jsonArray.getJSONObject(0).getInt("quantity"));

                                //Add item to the shopping cart
                                addItem(itemDTO);

                            } catch (JSONException e) {
                                Toast.makeText(PurchaseForm.this, "This item is not available", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, Integer.parseInt(data.getStringExtra("SCAN_RESULT")));
                } catch (NumberFormatException e){
                    Toast.makeText(PurchaseForm.this, "Invalid QR code", Toast.LENGTH_SHORT).show();
                }
            } else if (resultCode == RESULT_CANCELED) {

            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase_form);

        //Button
        btnAddToCart = (Button) findViewById(R.id.btnPurchaseMenuAddToCart);
        btnCheckOut = (Button) findViewById(R.id.btnPurchaseFormCheckOut);

        //TextView
        txtTotal = (TextView) findViewById(R.id.txtPurchaseFormTotal);

        //ListView
        lstItem = (ListView) findViewById(R.id.lstPurchaseFormItemList);

        itemList = new ArrayList<ItemDTO>();
        itemHash = new HashMap<>();
        adapter = new ItemBaseAdapter(ContextObject.GetContext(), itemList);
        lstItem.setAdapter(adapter);

        btnAddToCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addItemToCart();
            }
        });

        lstItem.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            }
        });

        lstItem.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                deleteItem(position);
                return true;
            }
        });

        btnCheckOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkOut();
            }
        });
    }

    private void updateTotal(double itemPrice){
        //update total according to the item addition/removal
        total += itemPrice;
        txtTotal.setText("Total : Rs. " + String.valueOf(total));
    }

    private void addItem(ItemDTO item){
        int i;
        boolean found = false;

        //Search for existing item
        for(i=0; i<itemList.size(); i++){
            if(item.getId() == itemList.get(i).getId()){
                found = true;
                break;
            }
        }

        if(found){
            //Item already exists in the shopping cart
            Toast.makeText(ContextObject.GetContext(), "Item already exists", Toast.LENGTH_SHORT).show();
        }else{
            //Add item to the list
            itemList.add(item);

            //Update the total
            updateTotal(item.getPrice());

            //Update item list
            adapter.notifyDataSetChanged();
        }

    }
}
