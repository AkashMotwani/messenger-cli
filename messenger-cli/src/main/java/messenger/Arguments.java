/*
 * Copyright 2015 Dirk Franssen.
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
package messenger;

/**
 * Supported arguments for the CLI.
 * 
 * @author dfranssen
 */
public enum Arguments {
    USER("-u"),
    PASSWORD("-p"),
    EMAIL("-e"),
    MESSAGE("-m"),
    TO("-t"),
    SERVER("-s"),
    MESSAGE_ID("-id"),
    INIT_ACTION("--init"),
    HELP_ACTION("-h"),
    SEND_ACTION("--send"),
    STATUS_ACTION("--status");
    
    private final String name;
    
    Arguments(String name) {
        this.name = name;
    }
    
    public String argumentName() {
        return name;
    }
}