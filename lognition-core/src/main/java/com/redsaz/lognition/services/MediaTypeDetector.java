/*
 * Copyright 2021 Redsaz <redsaz@gmail.com>.
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
package com.redsaz.lognition.services;

import com.redsaz.lognition.api.exceptions.AppServerException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

/**
 * Detects the media type. Currently this only operates on filename and mime magic.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class MediaTypeDetector {

  private final TikaConfig tika;

  public MediaTypeDetector() {
    try {
      tika = new TikaConfig();
    } catch (TikaException | IOException ex) {
      throw new AppServerException("Failed to initialize media type detector", ex);
    }
  }

  public String detect(InputStream source, String filename) {
    Metadata meta = new Metadata();
    if (filename != null) {
      meta.set(Metadata.RESOURCE_NAME_KEY, filename);
    }

    try (TikaInputStream tis = TikaInputStream.get(source)) {
      return tika.getDetector().detect(tis, meta).getBaseType().toString();
    } catch (IOException ex) {
      throw new AppServerException(
          "Error detecting media type for file=" + source + " filename=" + filename, ex);
    }
  }

  public String detect(File source, String filename, String suggestedMimeType) {
    Metadata meta = new Metadata();
    if (filename != null) {
      meta.set(Metadata.RESOURCE_NAME_KEY, filename);
    }
    if (suggestedMimeType != null) {
      meta.set(Metadata.CONTENT_TYPE, suggestedMimeType);
    }

    try (TikaInputStream tis = TikaInputStream.get(source)) {
      return tika.getDetector().detect(tis, meta).getBaseType().toString();
    } catch (IOException ex) {
      throw new AppServerException(
          "Error detecting media type for file="
              + source
              + " filename="
              + filename
              + " suggestedMimeType="
              + suggestedMimeType,
          ex);
    }
  }

  /**
   * Retrieves the basetype (the type and subtype) of the media type, leaving off anything after and
   * including the first semicolon.
   *
   * <p>Example: image/jpeg;foo=bar becomes image/jpeg
   *
   * @param type the media type string as received from the header.
   * @return the type and subtype
   */
  public String getBaseType(String type) {
    return MediaType.parse(type).getBaseType().toString();
  }
}
