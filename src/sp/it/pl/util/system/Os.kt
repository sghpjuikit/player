/*
 * Loosely based on ControlsFX Platform enum implementation.
 */

/*
 * Original licence:
 *
 * Copyright (c) 2013, ControlsFX
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of ControlsFX, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL CONTROLSFX BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package sp.it.pl.util.system

/** Operating system. */
enum class Os {
    WINDOWS,
    OSX,
    UNIX,
    UNKNOWN;

    val isCurrent get() = this==current

    companion object {

        /** @return the current operating system */
        @JvmStatic val current: Os = run {
            val prop = { propertyName: String -> System.getProperty(propertyName) }
            val osName = prop("os.name")
            when {
                osName.indexOf("win")!=-1 -> WINDOWS
                osName.indexOf("mac")!=-1 -> OSX
                osName.startsWith("SunOS") -> UNIX
                osName.indexOf("nix")!=-1 -> UNIX
                osName.indexOf("freebsd")!=-1 -> UNIX
                osName.indexOf("nux")!=-1 && "android"!=prop("javafx.platform") && "Dalvik"!=prop("java.vm.name") -> {
                    UNIX    // Linux without Android
                }
                else -> UNKNOWN
            }
        }
    }

}
