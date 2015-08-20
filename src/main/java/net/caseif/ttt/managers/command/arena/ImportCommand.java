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
import static net.caseif.ttt.util.Constants.INFO_COLOR;

import net.caseif.ttt.TTTCore;
import net.caseif.ttt.managers.command.SubcommandHandler;
import net.caseif.ttt.util.FileUtil;

import net.caseif.flint.util.physical.Boundary;
import net.caseif.flint.util.physical.Location3D;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;

import java.io.File;

public class ImportCommand extends SubcommandHandler {

    public ImportCommand(CommandSender sender, String[] args) {
        super(sender, args, "ttt.arena.import");
    }

    @Override
    public void handle() {
        if (assertPermission()) {
            if (args.length > 1) {
                String worldName = null;
                //TODO: null-check
                for (File f : Bukkit.getWorldContainer().listFiles()) {
                    if (f.getName().equalsIgnoreCase(args[1])) {
                        worldName = f.getName();
                    }
                }
                if (worldName != null) {
                    if (FileUtil.isWorld(args[1])) {
                        World w = Bukkit.createWorld(new WorldCreator(worldName));
                        if (w != null) {
                            if (TTTCore.mg.getArena(worldName).isPresent()) {
                                //TODO: replace this message with something more accurate
                                TTTCore.locale.getLocalizable("error.arena.already-exists")
                                        .withPrefix(ERROR_COLOR.toString()).sendTo(sender);
                            }
                            Location l = w.getSpawnLocation();
                            TTTCore.mg.createArena(worldName,
                                    new Location3D(l.getBlockX(), l.getBlockY(), l.getBlockZ()), Boundary.INFINITE);
                            TTTCore.locale.getLocalizable("info.personal.arena.import.success")
                                    .withPrefix(INFO_COLOR.toString()).sendTo(sender);
                            return;
                        }
                    }
                }
                // this executes only if something goes wrong loading the world
                TTTCore.locale.getLocalizable("error.plugin.world-load").withPrefix(ERROR_COLOR.toString())
                        .sendTo(sender);
            } else {
                TTTCore.locale.getLocalizable("error.command.too-few-args").withPrefix(ERROR_COLOR.toString())
                        .sendTo(sender);
                sendUsage();
            }
        }
    }
}
