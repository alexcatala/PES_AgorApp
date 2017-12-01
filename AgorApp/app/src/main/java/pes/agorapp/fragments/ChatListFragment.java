package pes.agorapp.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import pes.agorapp.JSONObjects.BuyTransaction;
import pes.agorapp.JSONObjects.Chat;
import pes.agorapp.R;
import pes.agorapp.globals.PreferencesAgorApp;
import pes.agorapp.helpers.ChatsAdapter;
import pes.agorapp.helpers.ObjectsHelper;


public class ChatListFragment extends Fragment {
    private List<Chat> chats = new ArrayList<>();
    private PreferencesAgorApp prefs;
    private ChatsAdapter adapter;

    private OnFragmentInteractionListener mListener;

    public ChatListFragment() {
        // Required empty public constructor
    }


    public static ChatListFragment newInstance(String param1, String param2) {
        ChatListFragment fragment = new ChatListFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        prefs = new PreferencesAgorApp(getActivity());
        chats = requestChats();
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chat_list, container, false);
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
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


    public interface OnFragmentInteractionListener {
        void onChatSelected(Chat chat);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new ChatsAdapter(getActivity(), chats);
        // Attach the adapter to a ListView
        final ListView listView = (ListView) view.findViewById(R.id.listChats);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Chat chat = (Chat) listView.getItemAtPosition(position);
                mListener.onChatSelected(chat);
            }
        });
        listView.setAdapter(adapter);

        adapter.addAll(chats);
    }

    private List<Chat> requestChats() {
        List<Chat> chats = new ArrayList<>();
        List<BuyTransaction> transactions = ObjectsHelper.getFakeTransactions();

        for (BuyTransaction transaction : transactions) {
          Chat chat = new Chat();
          chat.setUser(transaction.getOtherUser(prefs.getId()));
          chat.setLastMessage(ObjectsHelper.getFakeMessage());
          chat.setLastMessageDate(ObjectsHelper.getFakeDate());

          chats.add(chat);
        }
        return chats;
    }


}