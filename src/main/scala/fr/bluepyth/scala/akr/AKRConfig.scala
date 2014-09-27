/**
 * Android Keystore Recovery
 * Copyright (C) 2013 Romain Sertelon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.bluepyth.scala.akr

import java.io.File

case class AKRConfig(
    minLength: Option[Int] = None, 
    from: Option[String] = None, 
    to: Option[String] = None,
    lettersOnly: Boolean = false,
    numbersOnly: Boolean = false,
    upperCase: Boolean = false,
    lowerCase: Boolean = false,
    passwordsPerSecond: Option[Int] = None,
    extraCharacters: Option[String] = None,
    keystore: Option[File] = None,
    wordlist: Option[File] = None)