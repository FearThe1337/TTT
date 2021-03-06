/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013-2019, Max Roncace <me@caseif.net>
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

package net.caseif.ttt.command.handler.use;

import net.caseif.ttt.TTTCore;
import net.caseif.ttt.command.handler.CommandHandler;
import net.caseif.ttt.util.config.ConfigKey;
import net.caseif.ttt.util.config.OperatingMode;
import net.caseif.ttt.util.constant.Color;
import net.caseif.ttt.util.constant.Stage;
import net.caseif.ttt.util.helper.gamemode.BanHelper;

import com.google.common.base.Optional;
import net.caseif.flint.arena.Arena;
import net.caseif.flint.round.JoinResult;
import net.caseif.flint.round.Round;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JoinCommand extends CommandHandler {

    public JoinCommand(CommandSender sender, String[] args) {
        super(sender, args);
    }

    @Override
    public void handle() {
        if (BanHelper.checkBan(((Player) sender).getUniqueId())) {
            return;
        }

        if (args.length < 2 && TTTCore.config.get(ConfigKey.OPERATING_MODE) != OperatingMode.DEDICATED) {
            TTTCore.locale.getLocalizable("error.command.too-few-args").sendTo(sender);
            return;
        }

        Optional<Arena> arena = TTTCore.config.get(ConfigKey.OPERATING_MODE) == OperatingMode.DEDICATED
                ? Optional.of(TTTCore.getDedicatedArena()) // default to dedicated arena
                : TTTCore.mg.getArena(args[1]); // otherwise get the arena from the arg
        if (!arena.isPresent()) {
            TTTCore.locale.getLocalizable("error.arena.dne").withPrefix(Color.ALERT).sendTo(sender);
            return;
        }

        Round round = arena.get().getOrCreateRound();

        if ((round.getLifecycleStage() == Stage.PLAYING || round.getLifecycleStage() == Stage.ROUND_OVER)
                && !TTTCore.config.get(ConfigKey.ALLOW_JOIN_AS_SPECTATOR)) {
            TTTCore.locale.getLocalizable("error.round.in-progress").withPrefix(Color.ALERT).sendTo(sender);
        }

        JoinResult result = round.addChallenger(((Player) sender).getUniqueId());
        if (result.getStatus() != JoinResult.Status.SUCCESS) {
            switch (result.getStatus()) {
                case ALREADY_IN_ROUND: {
                    TTTCore.locale.getLocalizable("error.round.inside").withPrefix(Color.ALERT).sendTo(sender);
                    break;
                }
                case INTERNAL_ERROR: {
                    throw new RuntimeException(result.getThrowable()); // sender is notified of internal error
                }
                case PLAYER_OFFLINE: {
                    TTTCore.locale.getLocalizable("error.round.player-offline").withPrefix(Color.ALERT).sendTo(sender);
                    break;
                }
                case ROUND_FULL: {
                    TTTCore.locale.getLocalizable("error.round.full").withPrefix(Color.ALERT).sendTo(sender);
                    break;
                }
                default: {
                    throw new AssertionError("Failed to determine reaosn for RoundJoinException. "
                            + "Report this immediately.");
                }
            }
        }
    }

}
