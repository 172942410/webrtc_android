package com.lianyun.webrtc.ui.adapter;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import com.lianyun.webrtc.R;
import com.lianyun.webrtc.bean.MessageBean;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ItemHolder> {
    private static final String TAG = "MessageAdapter";
    private List<MessageBean> list;
    private Activity activity;
    private LayoutInflater inflater;
    OnItemClickListener onItemClick;

    public MessageAdapter(Activity context, List<MessageBean> list) {
        this(context);
        this.list = list;
    }

    public MessageAdapter(Activity activity) {
        this.activity = activity;
        inflater = LayoutInflater.from(activity);
    }

    @NonNull
    @Override
    public ItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_message, parent, false);
        ItemHolder itemHolder = new ItemHolder(view);
        return itemHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ItemHolder holder, int position) {
        MessageBean messageBean = list.get(position);
        holder.setViewHolder(messageBean, position);
    }

    @Override
    public int getItemCount() {
        if (list == null) {
            return 0;
        }
        return list.size();
    }

    public void setData(List<MessageBean> list) {
        choosepos = -1;
        this.list = list;
        notifyDataSetChanged();
    }

    public List<MessageBean> getData() {
        return list;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClick = onItemClickListener;
    }

    int choosepos = -1;

    public void setChosePos(int choosepos) {
        this.choosepos = choosepos;
        notifyDataSetChanged();
    }

    public void clearData() {
        choosepos = -1;
        if (this.list != null) {
            this.list.clear();
        }
    }

    public void addItemRightString(String message) {
        addItemString(message, -1);
    }

    /**
     * 还需要解析 发送时间
     *
     * @param message
     */
    public void addItemLeftString(String message) {
        addItemString(message, 1);
    }

    public void addItemString(String message, int type) {
        if (TextUtils.isEmpty(message)) {
            return;
        }
        if (list == null) {
            list = new ArrayList<>();
        }
        MessageBean messageBean = new MessageBean(message);
        messageBean.type = type;
        if (type == -1) {
            messageBean.sendTime = System.currentTimeMillis();
        } else if (type == 1) {
            String msgInfo[] = message.split("\\|");
            if (msgInfo.length == 2) {
                try {
                    messageBean.sendTime = Long.parseLong(msgInfo[0]);
                    messageBean.content = msgInfo[1];
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "解析发送时间时异常错误：" + message);
                }
            }
            messageBean.receiveTime = System.currentTimeMillis();
        }
        list.add(messageBean);
//        notifyDataSetChanged();
    }

    //图片链接地址
    public void addItemDataPath(String message) {

    }

    //显示文件下载的进度
    public void showProgress(float progress) {

    }

    public void addItemLeftDataUri(Uri uri) {
        addItemDataUri(uri, 1);
    }

    public void addItemRightDataUri(Uri uri) {
        addItemDataUri(uri, -1);
    }

    public void addItemDataUri(Uri uri, int type) {
        if (uri == null) {
            return;
        }
        if (list == null) {
            list = new ArrayList<>();
        }
        MessageBean messageBean = new MessageBean("");
        messageBean.uri = uri;
        messageBean.type = type;
        messageBean.sendTime = System.currentTimeMillis();
        list.add(messageBean);
    }

    public void addItemBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        if (list == null) {
            list = new ArrayList<>();
        }
        MessageBean messageBean = new MessageBean("");
        messageBean.bitmap = bitmap;
        messageBean.type = 1;
        messageBean.receiveTime = System.currentTimeMillis();
        list.add(messageBean);
    }

    class ItemHolder extends RecyclerView.ViewHolder {
        View view;
        int position;
        TextView tvTime;
        TextView tvTag;
        TextView tvMessage;
        AppCompatImageView imageView;
        MessageBean messageBean;

        public ItemHolder(@NonNull View itemView) {
            super(itemView);
            view = itemView;
            tvTime = view.findViewById(R.id.item_log_time);
            tvTag = view.findViewById(R.id.item_log_tag);
            tvMessage = view.findViewById(R.id.item_log_message);
            imageView = view.findViewById(R.id.iv_image);
        }

        private void setViewHolder(MessageBean messageBean, int position) {
            this.messageBean = messageBean;
            this.position = position;
//            android:layoutDirection="rtl"
            if (messageBean.type == -1) {
                view.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                //右边
                if (messageBean.uri != null) {
                    imageView.setVisibility(View.VISIBLE);
                    imageView.setImageURI(messageBean.uri);
                } else if (messageBean.bitmap != null) {
                    imageView.setVisibility(View.VISIBLE);
                    imageView.setImageBitmap(messageBean.bitmap);
                } else {
                    imageView.setVisibility(View.GONE);
                }
                if (messageBean.sendTime > 0) {
                    tvTime.setVisibility(View.VISIBLE);
                    tvTime.setText("发送时间：" + getFormatTime(messageBean.sendTime));
                } else {
                    tvTime.setVisibility(View.INVISIBLE);
                }
                tvTag.setVisibility(View.INVISIBLE);
            } else if (messageBean.type == 1) {
                //左边显示
                if (messageBean.bitmap != null) {
                    imageView.setVisibility(View.VISIBLE);
                    imageView.setImageBitmap(messageBean.bitmap);
                } else if (messageBean.uri != null) {
                    imageView.setVisibility(View.VISIBLE);
                    imageView.setImageURI(messageBean.uri);
                } else {
                    imageView.setVisibility(View.GONE);
                }
                if (messageBean.sendTime > 0) {
                    tvTime.setVisibility(View.VISIBLE);
                    tvTime.setText("发送时间：" + getFormatTime(messageBean.sendTime));
                } else {
                    tvTime.setVisibility(View.INVISIBLE);
                }
                if (messageBean.receiveTime > 0) {
                    tvTag.setVisibility(View.VISIBLE);
                    tvTag.setText("接收时间：" + getFormatTime(messageBean.receiveTime));
                } else {
                    tvTag.setVisibility(View.INVISIBLE);
                }
            }
            tvMessage.setText(messageBean.content);
            if (choosepos == position) {
                view.setBackgroundResource(R.color.gray_1a);
            } else {
                view.setBackgroundResource(R.drawable.bg_item_left);
            }
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    choosepos = position;
                    notifyDataSetChanged();
                    if (onItemClick != null) {
                        onItemClick.onItemClick(view, messageBean, position);
                    }
                }
            });
        }
    }

    public String getFormatTime(long time) {
        return getFormatTime(time, "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 根据指定的时间戳，返回指定格式的日期时间
     *
     * @param time   时间戳
     * @param format 指定的日期格式<br>
     *               eg:<br>
     *               "yyyy-MM-dd HH:mm:ss"<br>
     *               "yyyy-MM-dd"<br>
     *               "yyyyMMddHHmmss"<br>
     *               "HH:mm:ss"<br>
     * @return
     */
    public String getFormatTime(long time, String format) {
        Date date = new Date(time);
        String strs = "1970-01-01 00:00:00";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            strs = sdf.format(date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return strs;

    }

    public interface OnItemClickListener {

        /**
         *
         */
        void onItemClick(View view, MessageBean messageBean, int position);

    }
}
