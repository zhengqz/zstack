package org.zstack.storage.primary;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.EntityEvent;
import org.zstack.core.db.EntityLifeCycleCallback;
import org.zstack.header.Component;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.volume.VolumeAO;
import org.zstack.header.volume.VolumeVO;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xing5 on 2016/5/10.
 */
public class DiskCapacityTracer implements Component {
    private static Logger logger = LogManager.getLogger("org.zstack.storage.primary.DiskCapacityTracer");
    private static Logger loggerd = LogManager.getLogger("org.zstack.storage.primary.DiskCapacityTracerDetails");

    @Autowired
    private DatabaseFacade dbf;

    private void printCallTrace() {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        List<String> lst = new ArrayList<String>();
        for (StackTraceElement el : elements) {
            if (el.getClassName().contains("org.zstack")) {
                lst.add(el.toString());
            }
        }
        loggerd.debug(StringUtils.join(lst, "\n"));
    }

    @Override
    public boolean start() {
        if (!PrimaryStorageGlobalProperty.CAPACITY_TRACKER_ON) {
            return true;
        }

        dbf.installEntityLifeCycleCallback(VolumeVO.class, EntityEvent.POST_PERSIST, new EntityLifeCycleCallback() {
            @Override
            public void entityLifeCycleEvent(EntityEvent evt, Object o) {
                VolumeVO vol = (VolumeVO) o;
                if (vol.getSize() != 0) {
                    String info = String.format("[Volume:Create][name=%s, uuid=%s, type=%s]: %s",
                            vol.getName(), vol.getUuid(), vol.getType(), vol.getSize());
                    logger.debug(info);
                    loggerd.debug(info);
                    printCallTrace();
                }
            }
        });
        dbf.installEntityLifeCycleCallback(VolumeVO.class, EntityEvent.POST_UPDATE, new EntityLifeCycleCallback() {
            @Override
            public void entityLifeCycleEvent(EntityEvent evt, Object o) {
                VolumeVO vol = (VolumeVO) o;
                VolumeAO pre = vol.getShadow();
                if (pre.getSize() != vol.getSize()) {
                    String info = String.format("[Volume:Update][name=%s, uuid=%s, type=%s]: %s --> %s",
                            vol.getName(), vol.getUuid(), vol.getType(), pre.getSize(), vol.getSize());
                    logger.debug(info);
                    loggerd.debug(info);
                    printCallTrace();
                }
            }
        });
        dbf.installEntityLifeCycleCallback(ImageCacheVO.class, EntityEvent.POST_PERSIST, new EntityLifeCycleCallback() {
            @Override
            public void entityLifeCycleEvent(EntityEvent evt, Object o) {
                ImageCacheVO img = (ImageCacheVO) o;
                String info = String.format("[ImageCache:Create][uuid=%s]: %s", img.getImageUuid(), img.getSize());
                logger.debug(info);
                loggerd.debug(info);
                printCallTrace();
            }
        });
        dbf.installEntityLifeCycleCallback(VolumeSnapshotVO.class, EntityEvent.POST_PERSIST, new EntityLifeCycleCallback() {
            @Override
            public void entityLifeCycleEvent(EntityEvent evt, Object o) {
                VolumeSnapshotVO s = (VolumeSnapshotVO) o;
                String info = String.format("[VolumeSnapshot:Create][name=%s, uuid=%s]: %s", s.getName(), s.getUuid(), s.getSize());
                logger.debug(info);
                loggerd.debug(info);
                printCallTrace();
            }
        });
        dbf.installEntityLifeCycleCallback(PrimaryStorageCapacityVO.class, EntityEvent.POST_UPDATE, new EntityLifeCycleCallback() {
            @Override
            public void entityLifeCycleEvent(EntityEvent evt, Object o) {
                PrimaryStorageCapacityVO c = (PrimaryStorageCapacityVO) o;
                PrimaryStorageCapacityVO pre = c.getShadow();
                if (c.getAvailableCapacity() != pre.getAvailableCapacity()) {
                    String info = String.format("[PrimaryStorageCapacity:Change][uuid=%s]: %s --> %s", pre.getUuid(), pre.getAvailableCapacity(),
                            c.getAvailableCapacity());
                    logger.debug(info);
                    loggerd.debug(info);
                    printCallTrace();
                }
            }
        });

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
