package com.senya.eatappserver.ui.best_deals;

import androidx.lifecycle.ViewModelProvider;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.ImageView;
import android.widget.Toast;

import com.senya.eatappserver.R;
import com.senya.eatappserver.adapter.MyBestDealsAdapter;
import com.senya.eatappserver.adapter.MyCategoriesAdapter;
import com.senya.eatappserver.model.BestDealsModel;
import com.senya.eatappserver.model.CategoryModel;
import com.senya.eatappserver.ui.category.CategoryViewModel;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dmax.dialog.SpotsDialog;

public class BestDealsFragment extends Fragment {

    private BestDealsViewModel mViewModel;

    Unbinder unbinder;
    @BindView(R.id.recycler_best_deal)
    RecyclerView recycler_best_deal;
    AlertDialog dialog;
    LayoutAnimationController layoutAnimationController;
    MyBestDealsAdapter adapter;

    List<BestDealsModel> bestDealsModels;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mViewModel =
                new ViewModelProvider(this).get(BestDealsViewModel.class);
        View root = inflater.inflate(R.layout.best_deals_fragment,container,false);

        unbinder = ButterKnife.bind(this, root);
        initViews();
        mViewModel.getMessageError().observe(getViewLifecycleOwner(), s -> {
            Toast.makeText(getContext(), ""+s, Toast.LENGTH_SHORT).show();
        });
        mViewModel.getBestDealsListMutable().observe(getViewLifecycleOwner(),list -> {
            bestDealsModels = list;
            adapter = new MyBestDealsAdapter(getContext(),bestDealsModels);
            recycler_best_deal.setAdapter(adapter);
            recycler_best_deal.setLayoutAnimation(layoutAnimationController);
        });

        return root;
    }

    private void initViews() {
        dialog = new SpotsDialog.Builder().setContext(getContext()).setCancelable(false).build();
        layoutAnimationController = AnimationUtils.loadLayoutAnimation(getContext(),R.anim.layout_item_from_left);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recycler_best_deal.setLayoutManager(layoutManager);
        recycler_best_deal.addItemDecoration(new DividerItemDecoration(getContext(),layoutManager.getOrientation()));
    }


}