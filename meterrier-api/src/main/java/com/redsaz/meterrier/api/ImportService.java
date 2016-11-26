/*
 * Copyright 2016 Redsaz <redsaz@gmail.com>.
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
package com.redsaz.meterrier.api;

import com.redsaz.meterrier.api.model.ImportInfo;
import java.io.InputStream;
import java.util.List;

/**
 * Stores, accesses, processes pending imports of logs.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public interface ImportService {

    public ImportInfo upload(InputStream raw, ImportInfo source);

    public ImportInfo get(long id);

    public List<ImportInfo> list();

    public ImportInfo update(ImportInfo source);

    public void delete(long id);
}
