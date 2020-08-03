package example;

import arc.Events;
import arc.math.Mathf;
import arc.util.Log;
import arc.util.Time;
import mindustry.content.Blocks;
import mindustry.entities.type.Player;
import mindustry.game.Schematic;
import mindustry.game.Schematics;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.type.ItemStack;
import mindustry.world.Pos;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;

import java.util.HashMap;

import static mindustry.Vars.state;
import static mindustry.Vars.world;

public class Cell {
    protected int x;
    protected int y;
    public BuildRecorder recorder;
    protected Team owner = null;
    protected HashMap<Team, Integer> captureProgress = new HashMap<>();
    protected HashMap<Tuple<Float, Float>, Integer> builds = new HashMap<>();

    private Schematic spawn = Schematics.readBase64("bXNjaAB4nD2SYW7DIAyFbRJCIN2PHiRSr7ITTChDUyVKqjTptNsPB3iNFD4F+z37qTTRB1Of/COQieEd4tdtpGlZn8+wzb8+Rrru992n+/GYlzW9w9+60fW1Rr/NT59CnDP9BLos6xbmdCwxHC/q/LaQeS1+38NG45Hi6r8zDY+Q5CT6pPbr5MUgBSovuemp1WjQADKgEaIWog40gS6VuFar8xHg/K25MdwYbgw3rm7S6ergXJTbFmedgp6CnoKegp7C9AoqHU5dO4TbbY+zL5uwznQ685BJMmBlMp3zdVLXMuihopGZhoou+Sn51jo0OtokfabSYTI13+GcmlluW++AXoMdx5q4UMvPoK5lISmfeyohDRpABjRWlREqFioWKhYqFioWKhYqFioOCbm6r5AClT8wZ2oeDh4OHg4eDh6uTNgJOdAEKhP8A1h6KiA=");

    public Cell(int x, int y, BuildRecorder recorder){
        this.x = x;
        this.y = y;
        this.recorder = recorder;
    }

    public void makeNexus(CustomPlayer ply){
        placeSchem(x, y, spawn, true, ply);

        world.tile(x+6, y+6).setNet(Blocks.powerSource, owner, 0);

        /*clearSpace(Blocks.coreNucleus.size);
        Tile coreTile = world.tile(x, y);
        coreTile.setNet(Blocks.coreNucleus, owner, 0);
        for(ItemStack stack : state.rules.loadout){
            Call.transferItemTo(stack.item, stack.amount, coreTile.drawx(), coreTile.drawy(), coreTile);
        }*/
    }

    public void makeShard(){
        clearSpace(Blocks.coreShard.size);
        Tile coreTile = world.tile(x, y);
    }

    public void clearSpace(int size){
        for(int xi = -size/2; xi < size/2; xi++){
            for(int yi = -size/2; yi < size/2; yi ++){
                world.tile(x+xi, y+yi).link().removeNet();
            }
        }
    }

    public boolean contains(short x, short y){
        return Math.sqrt(Math.pow(this.x - x, 2) + Math.pow(this.y - y, 2)) < Assimilation.cellRadius - 3;
    }

    public void updateCapture(Tile tile, boolean breaking){
        Team tileTeam = tile.getTeam();
        int progress = 0;
        if(captureProgress.containsKey(tile.getTeam())){
            progress = captureProgress.get(tile.getTeam());
        }
        Tuple<Float, Float> pos = new Tuple<>(tile.worldx(), tile.worldy());

        if(breaking){
            if(builds.containsKey(pos)) progress -= builds.remove(pos);
            else return;
        }else{
            progress += tile.block().health;
            builds.put(pos, tile.block().health);
        }
        if(progress > Assimilation.cellRequirement && owner == null){
            owner = tile.getTeam();
            captureProgress.clear();
            Events.fire(new CellCaptureEvent(this));
        }else{
            captureProgress.put(tileTeam, progress);
        }

    }

    public void clearCell(){
        for(Tuple key : builds.keySet()){
            Float x = (Float) key.get(0);
            Float y = (Float) key.get(1);
            Tile tile = world.tileWorld(x, y);
            if(tile.entity != null) Time.run(Mathf.random(60f), tile.entity::kill);
        }
        owner = null;
        builds.clear();
        captureProgress.clear();
    }

    void placeSchem(int x, int y, Schematic schem, boolean addItem, CustomPlayer ply){
        Schematic.Stile coreTile = schem.tiles.find(s -> s.block instanceof CoreBlock);
        if(coreTile == null) throw new IllegalArgumentException("Schematic has no core tile. Exiting.");
        int ox = x - coreTile.x, oy = y - coreTile.y;
        schem.tiles.each(st -> {
            Tile tile = world.tile(st.x + ox,st.y + oy);
            if(tile == null) return;

            if(tile.link().block() != Blocks.air){
                tile.link().removeNet();
            }

            tile.setNet(st.block, owner, st.rotation);

            Tuple<Float, Float> pos = new Tuple<>(tile.worldx(), tile.worldy());
            builds.put(pos, tile.block().health);

            if(ply != null && !(tile.block() instanceof CoreBlock)){
                recorder.addBuild(tile.x, tile.y, ply, tile.block());
            }

            if(st.block.posConfig){
                tile.configureAny(Pos.get(tile.x - st.x + Pos.x(st.config), tile.y - st.y + Pos.y(st.config)));
            }else{
                tile.configureAny(st.config);
            }
            if(tile.block() instanceof CoreBlock && addItem){
                for(ItemStack stack : state.rules.loadout){
                    Call.transferItemTo(stack.item, stack.amount, tile.drawx(), tile.drawy(), tile);
                }
            }
        });
    }

    public static class CellCaptureEvent{
        public final Cell cell;

        public CellCaptureEvent(Cell cell){
            this.cell = cell;
        }
    }
}