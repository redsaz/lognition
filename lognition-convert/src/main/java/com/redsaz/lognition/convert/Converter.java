/*
 * Copyright 2017 Redsaz <redsaz@gmail.com>.
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
package com.redsaz.lognition.convert;

import java.io.File;

/**
 * Read in the source file, convert it, and write the conversion into dest.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public interface Converter {

  /**
   * Convert the source file into the dest file, and returns the SHA-256 hash (lowercase hex) as a
   * string.
   *
   * @param source File location of what needs converted
   * @param dest Where the result will be put
   * @return SHA-256 hash of the destination content as a string (lowercase hex)
   */
  String convert(File source, File dest);
}
