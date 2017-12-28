package pes.agorapp.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.gson.JsonObject;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import pes.agorapp.JSONObjects.Botiga;
import pes.agorapp.JSONObjects.Location;
import pes.agorapp.JSONObjects.Trophy;
import pes.agorapp.JSONObjects.UserAgorApp;
import pes.agorapp.R;
import pes.agorapp.activities.LoginActivity;
import pes.agorapp.customComponents.DialogServerKO;
import pes.agorapp.globals.PreferencesAgorApp;
import pes.agorapp.customComponents.MyGridView;
import pes.agorapp.helpers.ObjectsHelper;
import pes.agorapp.adapters.TrophiesAdapter;
import pes.agorapp.network.AgorAppApiManager;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Created by marc on 31/10/17.
 */

public class ProfileFragment extends Fragment implements View.OnClickListener {

    private AnnouncementListFragment.OnFragmentInteractionListener mListener;
    private PreferencesAgorApp prefs;
    private Location locationBotiga;
    private Dialog dialogForm;

    public ProfileFragment() {}

    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        prefs = new PreferencesAgorApp(getActivity());
        Toast.makeText(getActivity(), prefs.getId(), Toast.LENGTH_LONG).show();
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //wallet
        String walletText = prefs.getCoins() + " AgoraCoins";
        TextView wallet = (TextView) view.findViewById(R.id.profile_wallet_coins);
        wallet.setText(walletText);
        //botons
        view.findViewById(R.id.profile_btn_logout).setOnClickListener(this);
        view.findViewById(R.id.profile_btn_marketplace).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onMarketplaceOpen();
            }
        });

        view.findViewById(R.id.profile_btn_verify).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogForm = new Dialog(getActivity());
                dialogForm.setContentView(R.layout.form_verify_merchant);
                dialogForm.show();

                final EditText etNameBotiga = (EditText) dialogForm.findViewById(R.id.form_verify_titleEdit);
                final EditText etDescriptionBotiga = (EditText) dialogForm.findViewById(R.id.form_verify_descriptionEdit);
                Button verifyButton = (Button) dialogForm.findViewById(R.id.btn_form_verify_publish);

                verifyButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        upgradeAndCreateBotiga(etNameBotiga, etDescriptionBotiga);
                    }
                });

                PlaceAutocompleteFragment autocompFrag = (PlaceAutocompleteFragment) getActivity().getFragmentManager().findFragmentByTag("place_verify_form");

                AutocompleteFilter typeFilter = new AutocompleteFilter.Builder().setTypeFilter(AutocompleteFilter.TYPE_FILTER_ADDRESS).build();
                autocompFrag.setFilter(typeFilter);

                autocompFrag.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                    @Override
                    public void onPlaceSelected(Place place) {
                        locationBotiga = new Location(place.getLatLng().latitude,place.getLatLng().longitude);
                    }

                    @Override
                    public void onError(Status status) {
                        //Log.i(TAG, "An error occurred: " + status);
                    }
                });

                dialogForm.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        android.app.FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction();
                        ft.remove(getActivity().getFragmentManager().findFragmentByTag("place_verify_form"));
                        ft.commit();
                    }
                });
            }
        });

        //info d'usuari i imatge
        printProfile(view);

        List<Trophy> trophies = new ArrayList<>();
        TrophiesAdapter adapter = new TrophiesAdapter(getActivity(), trophies);
        final MyGridView gridView = (MyGridView) view.findViewById(R.id.gridview);
        gridView.setAdapter(adapter);
        trophies = ObjectsHelper.getFakeTrophies();
        adapter.addAll(trophies);
    }

    private void upgradeAndCreateBotiga(EditText etNameBotiga, EditText etDescriptionBotiga) {
        final int user_id = Integer.valueOf(prefs.getId());
        final String active_token = prefs.getActiveToken();

        String nameBotiga = etNameBotiga.getText().toString();
        String descriptionBotiga = etDescriptionBotiga.getText().toString();

        if (!validateForm(nameBotiga, descriptionBotiga, locationBotiga)) {
            //dialogForm.dismiss();
            final android.app.AlertDialog alertDialogValidate = new android.app.AlertDialog.Builder(getActivity()).create();
            alertDialogValidate.setTitle("Error");
            alertDialogValidate.setMessage("S'han d'omplir tots els camps");
            alertDialogValidate.setIcon(R.drawable.ic_warning_black_24dp);

            alertDialogValidate.setButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    alertDialogValidate.dismiss();
                }
            });

            alertDialogValidate.show();
        } else {
            JsonObject json = new JsonObject();
            json.addProperty("name", nameBotiga);
            json.addProperty("description", descriptionBotiga);
            json.addProperty("latitude", locationBotiga.getLatitude());
            json.addProperty("longitude", locationBotiga.getLongitude());
            json.addProperty("user_id", prefs.getId());

            JsonObject jsonBotiga = new JsonObject();
            jsonBotiga.add("shop", json);

            AgorAppApiManager
                    .getService()
                    .createShop(user_id, active_token, jsonBotiga)
                    .enqueue(new retrofit2.Callback<Botiga>() {
                        @Override
                        public void onResponse(Call<Botiga> call, Response<Botiga> response) {
                            Toast.makeText(getActivity(), response.body().getDescription(), Toast.LENGTH_LONG).show();
                            prefs.setShop(response.body().getId(), response.body().getName());

                            dialogForm.dismiss();

                            final android.app.AlertDialog alertDialogAnnouncementCreated = new android.app.AlertDialog.Builder(getActivity()).create();
                            alertDialogAnnouncementCreated.setTitle("Cupó creat");
                            alertDialogAnnouncementCreated.setMessage("Cupó afegit correctament");
                            alertDialogAnnouncementCreated.setIcon(R.drawable.ic_info_black_24dp);

                            alertDialogAnnouncementCreated.setButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    alertDialogAnnouncementCreated.dismiss();
                                }
                            });

                            alertDialogAnnouncementCreated.show();
                        }

                        @Override
                        public void onFailure(Call<Botiga> call, Throwable t) {
                            new DialogServerKO(getActivity()).show();
                        }
                    });
        }
    }

    private boolean validateForm(String nameBotiga, String descriptionBotiga, Location locationBotiga) {
        return !(nameBotiga.equals("") || descriptionBotiga.equals("") || locationBotiga==null);
    }

    private void printProfile(View view) {
        printTextProfile(view);
        printImageProfile(view);
        printOrHideVerifyButton(view);
    }

    private void printOrHideVerifyButton(View view) {
        /*amaguem el botó si ja està verificat*/
        if (prefs.hasShop()) {
            Button verifyButton = (Button) view.findViewById(R.id.profile_btn_verify);
            verifyButton.setVisibility(View.GONE);
        } else {
            TextView userType = (TextView) view.findViewById(R.id.profile_verified);
            userType.setVisibility(View.GONE);
        }
    }

    private void printImageProfile(View view) {
        /*imatge*/
        if (!prefs.getImageUrl().equals("www.imatgedummy.com") && !prefs.getImageUrl().equals("") && prefs.getImageUrl() != null) {
            Picasso.with(getActivity().getApplicationContext())
                    .load(prefs.getImageUrl())
                    .resize(250, 250)
                    .into((ImageView) view.findViewById(R.id.profile_user_img));
        } else {
            Picasso.with(getActivity().getApplicationContext())
                    .load(R.drawable.avatar_face_1_)
                    .resize(250, 250)
                    .into((ImageView) view.findViewById(R.id.profile_user_img));
        }
    }

    private void printTextProfile(View view) {
        /*info d'usuari*/
        String userName = prefs.getUserName();
        TextView profileNameTextView = (TextView) view.findViewById(R.id.profile_name);
        profileNameTextView.setText(userName);
        String userType = "Ciutadà";
        if (prefs.hasShop()) userType = "Comerciant verificat: " + prefs.getShopName();
        TextView profileTypeTextView = (TextView) view.findViewById(R.id.profile_verified);
        profileTypeTextView.setText(userType);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof AnnouncementListFragment.OnFragmentInteractionListener) {
            mListener = (AnnouncementListFragment.OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick (View v){
        switch (v.getId()) {
            case R.id.profile_btn_logout:
                close_session();
                break;
        }
    }

    private void close_session() {
        new AlertDialog.Builder(getActivity())
                .setTitle("Tancar sessió")
                .setMessage("N'estàs segur?")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        JsonObject jsonUser = new JsonObject();
                        jsonUser.addProperty("id", prefs.getId());
                        jsonUser.addProperty("active_token", prefs.getActiveToken());

                        AgorAppApiManager
                                .getService()
                                .logoutUser(jsonUser)
                                .enqueue(new retrofit2.Callback<UserAgorApp>() {
                                    @Override
                                    public void onResponse(Call<UserAgorApp> call, Response<UserAgorApp> response) {
                                        Log.i("codi resposta", String.valueOf(response.code()));

                                        prefs.deleteSession(); //Al fer logout, s'elimina la informació interna de l'app

                                        Intent i = new Intent(getActivity(), LoginActivity.class);
                                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); //Elimina totes less activities obertes
                                        startActivity(i);

                                        Toast.makeText(getActivity().getApplicationContext(), "LOGOUT correcte, token refrescat", Toast.LENGTH_LONG).show();
                                    }

                                    @Override
                                    public void onFailure(Call<UserAgorApp> call, Throwable t) {
                                        Toast.makeText(getActivity().getApplicationContext(), "FAIL al logout", Toast.LENGTH_LONG).show();
                                        new DialogServerKO(getActivity()).show();
                                    }
                                });
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
