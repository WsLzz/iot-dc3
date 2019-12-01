/*
 * Copyright 2019 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pnoker.common.model.rtmp;

import com.pnoker.common.model.Description;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * <p>Rtmp 信息实体类
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Rtmp extends Description {

    private String name;
    private String rtspUrl;
    private String rtmpUrl;
    private String command;
    private Short videoType;
    private Boolean run;
    private Boolean autoStart;
    private Long nodeId;
    private Long imageId;

    public Rtmp(long id, boolean run) {
        super.setId(id);
        this.run = run;
    }

    public Rtmp(boolean autoStart) {
        this.autoStart = autoStart;
    }

}