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
package net.caseif.ttt.managers.command.arena;

import static net.caseif.ttt.util.Constants.ERROR_COLOR;

import net.caseif.ttt.TTTCore;
import net.caseif.ttt.managers.command.SubcommandHandler;

import com.google.common.base.Optional;
import net.caseif.flint.arena.Arena;
import net.caseif.flint.exception.round.RoundJoinException;
import net.caseif.flint.round.Round;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JoinCommand extends SubcommandHandler {

    public JoinCommand(CommandSender sender, String[] args) {
        super(sender, args, "ttt.arena.join");
    }

    @Override
    public void handle() {
        if (sender instanceof Player) {
            if (assertPermission()) {
                if (args.length > 1) {
                    Optional<Arena> arena = TTTCore.mg.getArena(args[1]);
                    if (!arena.isPresent()) {
                        TTTCore.locale.getLocalizable("error.arena.dne").withPrefix(ERROR_COLOR.toString())
                                .sendTo(sender);
                        return;
                    }
                    Round round = arena.get().getRound().isPresent()
                            ? arena.get().getRound().get()
                            : arena.get().createRound();
                    try {
                        round.addChallenger(((Player) sender).getUniqueId());
                    } catch (RoundJoinException ex) {
                        switch (ex.getReason()) {
                            case ALREADY_ENTERED: {
                                TTTCore.locale.getLocalizable("error.round.inside").withPrefix(ERROR_COLOR.toString())
                                        .sendTo(sender);
                                break;
                            }
                            case FULL: {
                                TTTCore.locale.getLocalizable("error.round.full").withPrefix(ERROR_COLOR.toString())
                                        .sendTo(sender);
                                break;
                            }
                            case INTERNAL_ERROR: {
                                throw new RuntimeException(ex); // sender is notified of internal error
                            }
                            case OFFLINE: {
                                //TODO: message
                                break;
                            }
                            default: {
                                throw new AssertionError("Failed to determine reaosn for RoundJoinException. "
                                        + "Report this immediately.");
                            }
                        }
                    }
                } else {
                    TTTCore.locale.getLocalizable("error.command.too-few-args").withPrefix(ERROR_COLOR.toString())
                            .sendTo(sender);
                    sendUsage();
                }
            }
        } else {
            TTTCore.locale.getLocalizable("error.command.ingame").withPrefix(ERROR_COLOR.toString()).sendTo(sender);
        }
    }
}
