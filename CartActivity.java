package com.fitnessapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import com.fitnessapp.APIs.ApiClient;
import com.fitnessapp.APIs.ApiInterface;
import com.fitnessapp.Adapters.CartAdapter;
import com.fitnessapp.Dialogs.BlueLoadingDialogFragment;
import com.fitnessapp.Helpers.DataControllers;
import com.fitnessapp.Models.Cart.Cart;
import com.fitnessapp.Models.Cart.CartProduct;
import com.fitnessapp.Models.Cart.Checkout.CheckoutResponse;
import com.fitnessapp.Preferences.GymSubscriptionPeferences;
import com.fitnessapp.Widgets.Buttons.BoldBtn;
import com.fitnessapp.Widgets.TextViews.BoldTV;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import es.dmoral.toasty.Toasty;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartActivity extends AppCompatActivity {


    @BindView(R.id.btnBack)
    ImageView btnBack;

    @BindView(R.id.cartList)
    ListView cartList;

    @BindView(R.id.txttotal)
    BoldTV txttotal;

    @BindView(R.id.txtEdit)
    BoldTV txtEdit;

    @BindView(R.id.btnCheckout)
    BoldBtn btnCheckout;

    DataControllers dataControllers;
    Cart cart;
    public static ArrayList<CartProduct> cartProducts = new ArrayList<>();
    CartAdapter cartAdapter;
    BlueLoadingDialogFragment blueLoadingDialogFragment;
    CheckoutResponse checkoutResponse;
    Map<String, String> prods;
    int totalPr = 0;

    boolean isEdit = false;

    GymSubscriptionPeferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);
        initUI();
    }

    public void initUI() {
        pref = new GymSubscriptionPeferences(CartActivity.this);
        ButterKnife.bind(this);
        if (new DataControllers().getLang(this) != null && new DataControllers().getLang(this).equals("ar")) {
            btnBack.setRotation(180);
        }
        dataControllers = new DataControllers();
        updateCartObject();

        updateView();
    }

    public void updateCartObject() {
        cart = dataControllers.retrieveUserCart(CartActivity.this);
        if (cart == null) {
            cart = new Cart();
            cartProducts = new ArrayList<>();
            Toasty.error(CartActivity.this, getResources().getString(R.string.cart_empty)).show();
            finish();
        } else {
            cartProducts = cart.getCartProducts();
        }
        txtEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isEdit) {
                    for (int i = 0; i < cartProducts.size(); i++) {
                        cartProducts.get(i).setDeletable(true);
                    }
                    cartAdapter.notifyDataSetChanged();
                    isEdit = true;
                    txtEdit.setText(getResources().getString(R.string.done_text));
                } else {
                    for (int i = 0; i < cartProducts.size(); i++) {
                        cartProducts.get(i).setDeletable(false);
                    }
                    cartAdapter.notifyDataSetChanged();
                    isEdit = false;
                    txtEdit.setText(getResources().getString(R.string.edit_text));
                }
            }
        });
        btnCheckout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

MainActivity.fromCart = true;
                startActivity(new Intent(CartActivity.this, MyAddressesActivity.class));

//                showAddress(); 04.04.2018

//                setupCheckOut(); 03.04.2018
            }
        });
    }


    private void showAddress(){
        final AlertDialog.Builder alert = new AlertDialog.Builder(this, R.style.MyAlertDialogTheme);

        alert.setTitle("Address");
        alert.setMessage("Please check your address");
        alert.setCancelable(true);

// Set an EditText view to get user input
        final EditText input = new EditText(this);
        alert.setView(input);

        input.setTextColor(getResources().getColor(R.color.white));

        if (pref.getAddress().length() != 0){
            input.setText(pref.getAddress());
        }

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                if (input.getText().toString().trim().length() == 0){
                    input.setError("empty field");
                }else {
                    pref.setAddress(input.getText().toString().trim());

                    Intent intent = new Intent(CartActivity.this, SuplimentsConfirmationActivity.class);

                    startActivity(intent);
                }
                // Do something with value!
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }

    public void updateView() {
        cartAdapter = new CartAdapter(CartActivity.this, cartProducts, CartActivity.this);
        cartList.setAdapter(cartAdapter);

        for (int i = 0; i < cartProducts.size(); i++) {
            totalPr += (cartProducts.get(i).getCost() * cartProducts.get(i).getQuantity());
        }
        txttotal.setText(String.valueOf(totalPr) + " KWD");

    }

    public void setupCheckOut() {
        prods = new HashMap<>();
        for (int i = 0; i < cartProducts.size(); i++) {
            prods.put("products[" + i + "][item_id]", String.valueOf(cartProducts.get(i).getId()));
            prods.put("products[" + i + "][qunt]", String.valueOf(cartProducts.get(i).getQuantity()));
        }
        doCheckOut();
    }

    public void doCheckOut() {
        blueLoadingDialogFragment = new BlueLoadingDialogFragment();
        blueLoadingDialogFragment.setCancelable(false);
        blueLoadingDialogFragment.show(getSupportFragmentManager(), "Loading");
        checkoutResponse = new CheckoutResponse();
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        Call<CheckoutResponse> call = apiInterface.checkOut(
                dataControllers.RetrieveUserObject(CartActivity.this).getUserId(),
                6,
                String.valueOf(totalPr),
                prods,
                "BOTH",
                "KWD"
        );
        call.enqueue(new Callback<CheckoutResponse>() {
            @Override
            public void onResponse(Call<CheckoutResponse> call, Response<CheckoutResponse> response) {
                blueLoadingDialogFragment.dismiss();
                checkoutResponse = response.body();
                if (checkoutResponse != null) {
                    if (checkoutResponse.getStatus() && checkoutResponse.getCode() == 200) {
                        showWebview();
                    } else {
                        Toasty.error(CartActivity.this, "Couldn't get data , try again later").show();
                    }
                } else {
                    Toasty.error(CartActivity.this, "Couldn't get data , try again later").show();
                }
            }

            @Override
            public void onFailure(Call<CheckoutResponse> call, Throwable t) {
                blueLoadingDialogFragment.dismiss();
                Log.e("TAG", "onFailure");
                Toasty.error(CartActivity.this, "Couldn't get data , try again later").show();
            }
        });
    }

    public void showWebview() {
        Intent intent = new Intent(CartActivity.this, WebViewActivity.class);
        intent.putExtra("intentURL", checkoutResponse.getData().getPaymentURL());
        startActivityForResult(intent, 99);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 99) {
            if (resultCode == RESULT_OK) {

                int result = Integer.parseInt(data.getStringExtra("result"));
                if (result == 1) {
                    dataControllers.saveUserCart(CartActivity.this, null);
                    Toasty.success(CartActivity.this,
                            getResources().getString(R.string.success_payment)).show();
                } else {
                    Toasty.error(CartActivity.this,
                            getResources().getString(R.string.faild_payment)).show();
                }
                finish();
            }
        }
    }

    public void onBack(View view) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    public void updateCart(int pos) {
        cartProducts.remove(pos);
        if (cartProducts.size() == 0) {
            dataControllers.saveUserCart(CartActivity.this, null);
            finish();
        } else {
            cart.setCartProducts(cartProducts);
            dataControllers.saveUserCart(CartActivity.this, cart);
            cartAdapter.notifyDataSetChanged();
            totalPr = 0;
            for (int i = 0; i < cartProducts.size(); i++) {
                totalPr += (cartProducts.get(i).getCost() * cartProducts.get(i).getQuantity());
            }
            txttotal.setText(String.valueOf(totalPr) + " KWD");
        }


    }
}
