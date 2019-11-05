package com.example.myapplication3.Utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.example.myapplication3.R;

import java.util.List;

/**
 * Created by weizheng.huang on 2019-11-04.
 */
public class CheckListAdapter extends BaseAdapter {
    private List<CheckBox>  list;
    private Context mContext;

    public void setCheckListObserver(CheckListObserver checkListObserver) {
        this.checkListObserver = checkListObserver;
    }

    private CheckListObserver checkListObserver;
    public CheckListAdapter(List<CheckBox> list, Context mContext) {
        this.list = list;
        this.mContext = mContext;
    }

    public interface CheckListObserver{
        public void checkClick(boolean isChecked,int position);
    }
    @Override
    public int getCount() {
        return list.size() > 0 ? list.size() : 0;
    }

    @Override
    public Object getItem(int i) {
        return list.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;

        if (view == null){
            viewHolder = new ViewHolder();
            view = LayoutInflater.from(mContext).inflate(R.layout.checklistitem,viewGroup,false);
            viewHolder.checkBox = view.findViewById(R.id.checkbox);
            view.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder) view.getTag();
        }
        viewHolder.checkBox.setText(list.get(i).getText());
        viewHolder.checkBox.setChecked(list.get(i).isChecked());
        viewHolder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (checkListObserver != null) {
                    checkListObserver.checkClick(b, i);
                }
            }
        });
        return view;
    }


    class ViewHolder{
        CheckBox checkBox;
    }
}
