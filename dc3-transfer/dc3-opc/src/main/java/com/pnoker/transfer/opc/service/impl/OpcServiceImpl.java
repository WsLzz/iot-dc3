package com.pnoker.transfer.opc.service.impl;

import com.pnoker.transfer.opc.bean.OpcInfo;
import com.pnoker.transfer.opc.bean.OpcItem;
import com.pnoker.transfer.opc.bean.OpcNodes;
import com.pnoker.transfer.opc.bean.OpcServer;
import com.pnoker.transfer.opc.service.OpcService;
import org.jinterop.dcom.common.JIException;
import org.openscada.opc.dcom.list.ClassDetails;
import org.openscada.opc.lib.common.AlreadyConnectedException;
import org.openscada.opc.lib.common.ConnectionInformation;
import org.openscada.opc.lib.da.Server;
import org.openscada.opc.lib.da.browser.Branch;
import org.openscada.opc.lib.da.browser.Leaf;
import org.openscada.opc.lib.list.Categories;
import org.openscada.opc.lib.list.Category;
import org.openscada.opc.lib.list.ServerList;
import org.springframework.stereotype.Service;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: Opc 服务接口实现类
 */
@Service
public class OpcServiceImpl implements OpcService {
    /**
     * 查询指定主机Opc服务列表
     *
     * @param info
     * @return
     * @throws JIException
     * @throws UnknownHostException
     */
    @Override
    public List<OpcServer> opcServerList(OpcInfo info) throws JIException, UnknownHostException {
        if (null == info.getDomain() || "null".equals(info.getDomain()) || "".equals(info.getDomain().trim())) {
            info.setDomain("");
        }
        ServerList serverList = new ServerList(info.getHost(), info.getUser(), info.getPassword(), info.getDomain());
        Collection<ClassDetails> detailsCollection = serverList.listServersWithDetails(new Category[]{Categories.OPCDAServer10, Categories.OPCDAServer20, Categories.OPCDAServer30}, new Category[]{});
        List<OpcServer> opcServerList = new ArrayList<>();
        for (ClassDetails details : detailsCollection) {
            opcServerList.add(new OpcServer(details.getClsId(), details.getProgId(), details.getDescription()));
        }
        return opcServerList;
    }

    @Override
    public OpcNodes opcItemList(OpcInfo info) throws AlreadyConnectedException, JIException, UnknownHostException {
        OpcNodes opcNodes = new OpcNodes("Root", "root", new ArrayList<>());
        if (null == info.getDomain() || "null".equals(info.getDomain()) || "".equals(info.getDomain().trim())) {
            info.setDomain("");
        }
        ConnectionInformation ci = new ConnectionInformation(info.getUser(), info.getPassword());
        ci.setHost(info.getHost());
        ci.setDomain(info.getDomain());
        ci.setClsid(info.getClsId());

        Server server = new Server(ci, Executors.newSingleThreadScheduledExecutor());
        server.connect();
        dumpTree(server.getTreeBrowser().browse(), opcNodes);
        server.disconnect();
        return opcNodes;
    }

    @Override
    public OpcItem syncRead() {
        return null;
    }

    @Override
    public OpcItem asyncRead() {
        return null;
    }

    @Override
    public int syncWrite() {
        return 0;
    }

    @Override
    public int asyncWrite() {
        return 0;
    }

    public void dumpTree(final Branch branch, OpcNodes opcNodes) {
        for (final Leaf leaf : branch.getLeaves()) {
            OpcNodes item = new OpcNodes(leaf.getItemId(), "node");
            opcNodes.getChildren().add(item);
        }
        for (final Branch subBranch : branch.getBranches()) {
            OpcNodes item = new OpcNodes(subBranch.getName(), "group", new ArrayList<>());
            opcNodes.getChildren().add(item);
            dumpTree(subBranch, item);
        }
    }
}