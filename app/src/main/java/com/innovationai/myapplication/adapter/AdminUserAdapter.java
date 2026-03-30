package com.innovationai.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.innovationai.myapplication.R;
import com.innovationai.myapplication.model.User;

import java.util.List;

/**
 * 管理员用户列表适配器
 */
public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.UserViewHolder> {
    public interface OnUserActionListener {
        void onDeleteUser(User user);
    }

    private List<User> users;
    private final OnUserActionListener listener;

    public AdminUserAdapter(List<User> users, OnUserActionListener listener) {
        this.users = users;
        this.listener = listener;
    }

    public void updateUsers(List<User> newUsers) {
        this.users = newUsers;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.bind(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users == null ? 0 : users.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameText;
        private final TextView subtitleText;
        private final MaterialButton deleteButton;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.admin_user_name);
            subtitleText = itemView.findViewById(R.id.admin_user_subtitle);
            deleteButton = itemView.findViewById(R.id.admin_delete_user_button);
        }

        void bind(User user) {
            nameText.setText(user.getName());
            subtitleText.setText(user.getEmail() + " | " + user.getCredits() + "积分 | " + user.getRole());
            deleteButton.setOnClickListener(v -> listener.onDeleteUser(user));
        }
    }
}
