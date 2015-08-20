/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013-2015, Maxim Roncacé <mproncace@lapis.blue>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.caseif.ttt.managers.command.admin;

import static net.caseif.ttt.util.Constants.ARENA_COLOR;
import static net.caseif.ttt.util.Constants.ERROR_COLOR;

import net.caseif.ttt.TTTCore;
import net.caseif.ttt.managers.command.SubcommandHandler;
import net.caseif.ttt.util.Constants;

import com.google.common.base.Optional;
import net.caseif.flint.arena.Arena;
import org.bukkit.command.CommandSender;

public class EndCommand extends SubcommandHandler {

    public EndCommand(CommandSender sender, String[] args) {
        super(sender, args, "ttt.admin.end");
    }

    @Override
    public void handle() {
        if (assertPermission()) {
            if (args.length > 1) {
                String arenaName = args[1];
                Optional<Arena> arena = TTTCore.mg.getArena(arenaName);
                if (arena.isPresent()) {
                    if (arena.get().getRound().isPresent()
                            && arena.get().getRound().get().getLifecycleStage() != Constants.WAITING) {
                        if (args.length > 2) {
                            if (args[2].equalsIgnoreCase("t")) {
                                arena.get().getRound().get().getMetadata().set("t-victory", true);
                            } else if (!args[2].equalsIgnoreCase("i")) {
                                TTTCore.locale.getLocalizable("error.command.invalid-args")
                                        .withPrefix(ERROR_COLOR.toString()).sendTo(sender);
                                return;
                            }
                        }
                        arena.get().getRound().get().end();
                    } else {
                        TTTCore.locale.getLocalizable("error.arena.no-round")
                        .withPrefix(ERROR_COLOR.toString()).withReplacements(ARENA_COLOR + arenaName).sendTo(sender);
                    }
                } else {
                    TTTCore.locale.getLocalizable("error.arena.dne")
                            .withPrefix(ERROR_COLOR.toString()).withReplacements(ARENA_COLOR + arenaName)
                            .sendTo(sender);
                }
            } else {
                TTTCore.locale.getLocalizable("error.command.too-few-args").withPrefix(ERROR_COLOR.toString())
                        .sendTo(sender);
                sendUsage();
            }
        }
    }
}
