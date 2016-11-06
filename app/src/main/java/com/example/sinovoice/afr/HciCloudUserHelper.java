package com.example.sinovoice.afr;

import com.sinovoice.hcicloudsdk.api.HciCloudUser;
import com.sinovoice.hcicloudsdk.common.HciErrorCode;

import java.util.ArrayList;

/**
 * Created by miaochangchun on 2016/11/6.
 */
public class HciCloudUserHelper {
    private static HciCloudUserHelper mHciCloudUserHelper = null;

    private HciCloudUserHelper(){

    }

    public static HciCloudUserHelper getInstance() {
        if (mHciCloudUserHelper == null) {
            return new HciCloudUserHelper();
        }
        return mHciCloudUserHelper;
    }

    /**
     * 添加某一用户到组
     * @param groupId   组名，本地只能设置为空
     * @param userId    用户名
     * @return  true是添加成功，false是添加失败
     */
    public boolean addUser(String groupId, String userId) {
        int errorCode = HciCloudUser.hciAddUser(groupId, userId);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            return false;
        }
        return true;
    }

    /**
     * 查询组内所有用户列表
     * @param groupId   组名
     * @return  把所有的用户列表保存到ArrayList里面
     */
    public ArrayList<String> getUserList(String groupId){
        ArrayList<String> lists = new ArrayList<>();
        int errorCode = HciCloudUser.hciGetUserlist(groupId, lists);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            return null;
        }
        return lists;
    }

    /**
     * 新建一个用户组
     * @param groupId   组Id
     * @return  true是新建成功，false是新建失败
     */
    public boolean createGroup(String groupId) {
        //第二个参数1是不共享，0是共享
        int errorCode = HciCloudUser.hciCreateGroup(groupId, 1);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            return false;
        }
        return true;
    }

    /**
     * 获取用户可访问的所有组列表，只有云端才可以使用此功能。
     * @return  返回所有的组列表，保存到ArrayList里面
     */
    public ArrayList<String> getGroupList(){
        ArrayList<String> lists = new ArrayList<>();
        int errorCode = HciCloudUser.hciGetGrouplist(lists);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            return null;
        }
        return lists;
    }

    /**
     * 只有云端识别才能使用此功能,解除用户和组的关系
     * @param groupId
     * @return  true是移除成功，false是移除失败
     */
    public boolean deleteGroup(String groupId) {
        int errorCode = HciCloudUser.hciDeleteGroup(groupId);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            return false;
        }
        return true;
    }

    /**
     * 从一个组中移除一个用户
     * @param groupId   组名
     * @param userId    用户名
     * @return  true是移除成功，false是移除失败
     */
    public boolean removeUser(String groupId, String userId) {
        int errorCode = HciCloudUser.hciRemoveUser(groupId, userId);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            return false;
        }
        return true;
    }
}
