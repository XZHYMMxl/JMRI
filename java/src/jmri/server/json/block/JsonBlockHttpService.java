package jmri.server.json.block;

import static jmri.server.json.block.JsonBlock.BLOCK;
import static jmri.server.json.block.JsonBlock.BLOCKS;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Locale;
import javax.servlet.http.HttpServletResponse;
import jmri.Block;
import jmri.BlockManager;
import jmri.InstanceManager;
import jmri.Reporter;
import jmri.ReporterManager;
import jmri.Sensor;
import jmri.SensorManager;
import jmri.server.json.JSON;
import jmri.server.json.JsonException;
import jmri.server.json.JsonNamedBeanHttpService;
import jmri.server.json.reporter.JsonReporter;
import jmri.server.json.sensor.JsonSensor;

/**
 *
 * @author mstevetodd Copyright 2018
 * @author Randall Wood Copyright 2018
 */
public class JsonBlockHttpService extends JsonNamedBeanHttpService {

    public JsonBlockHttpService(ObjectMapper mapper) {
        super(mapper);
    }

    @Override
    public JsonNode doGet(String type, String name, Locale locale) throws JsonException {
        Block block = InstanceManager.getDefault(BlockManager.class).getBlock(name);
        ObjectNode root = this.getNamedBean(block, name, type, locale); // throws JsonException if block == null
        ObjectNode data = root.with(JSON.DATA);
        if (block != null) {
            switch (block.getState()) {
                case Block.UNDETECTED:
                    data.put(JSON.STATE, JSON.UNKNOWN);
                    break;
                default:
                    data.put(JSON.STATE, block.getState());
            }
            if (block.getValue() == null) {
                data.putNull(JSON.VALUE);
            } else {
                data.put(JSON.VALUE, block.getValue().toString());
            }
            if (block.getSensor() == null) {
                data.putNull(JsonSensor.SENSOR);
            } else {
                data.put(JsonSensor.SENSOR, block.getSensor().getSystemName());
            }
            if (block.getReporter() == null) {
                data.putNull(JsonReporter.REPORTER);
            } else {
                data.put(JsonReporter.REPORTER, block.getReporter().getSystemName());
            }
        }
        return root;
    }

    @Override
    public JsonNode doPost(String type, String name, JsonNode data, Locale locale) throws JsonException {
        Block block = InstanceManager.getDefault(BlockManager.class).getBlock(name);
        if (block == null) {
            throw new JsonException(404, Bundle.getMessage(locale, "ErrorObject", BLOCK, name));
        }
        this.postNamedBean(block, data, name, type, locale);
        if (!data.path(JSON.VALUE).isMissingNode()) {
            if (data.path(JSON.VALUE).isNull()) {
                block.setValue(null);
            } else {
                block.setValue(data.path(JSON.VALUE).asText());
            }
        }
        int state = data.path(JSON.STATE).asInt(JSON.UNKNOWN);
        switch (state) {
            case JSON.ACTIVE:
                block.setState(Block.OCCUPIED);
                break;
            case JSON.INACTIVE:
                block.setState(Block.UNOCCUPIED);
                break;
            case JSON.UNKNOWN:
                // leave state alone in this case
                break;
            default:
                throw new JsonException(400, Bundle.getMessage(locale, "ErrorUnknownState", BLOCK, state));
        }
        if (!data.path(JsonSensor.SENSOR).isMissingNode()) {
            JsonNode node = data.path(JsonSensor.SENSOR);
            if (node.isNull()) {
                block.setSensor(null);
            } else {
                Sensor sensor = InstanceManager.getDefault(SensorManager.class).getBySystemName(node.asText());
                if (sensor != null) {
                    block.setSensor(sensor.getSystemName());
                } else {
                    throw new JsonException(404, Bundle.getMessage(locale, "ErrorNotFound", JsonSensor.SENSOR, node.asText()));
                }
            }
        }
        if (!data.path(JsonReporter.REPORTER).isMissingNode()) {
            JsonNode node = data.path(JsonReporter.REPORTER);
            if (node.isNull()) {
                block.setReporter(null);
            } else {
                Reporter reporter = InstanceManager.getDefault(ReporterManager.class).getBySystemName(node.asText());
                if (reporter != null) {
                    block.setReporter(reporter);
                } else {
                    throw new JsonException(404, Bundle.getMessage(locale, "ErrorNotFound", JsonReporter.REPORTER, node.asText()));
                }
            }
        }
        return this.doGet(type, name, locale);
    }

    @Override
    public JsonNode doPut(String type, String name, JsonNode data, Locale locale) throws JsonException {
        try {
            InstanceManager.getDefault(BlockManager.class).provideBlock(name);
        } catch (Exception ex) {
            throw new JsonException(500, Bundle.getMessage(locale, "ErrorCreatingObject", BLOCK, name));
        }
        return this.doPost(type, name, data, locale);
    }

    @Override
    public ArrayNode doGetList(String type, Locale locale) throws JsonException {
        ArrayNode root = this.mapper.createArrayNode();
        for (Block block : InstanceManager.getDefault(BlockManager.class).getNamedBeanSet()) {
            root.add(this.doGet(BLOCK, block.getSystemName(), locale));
        }
        return root;

    }

    @Override
    public JsonNode doSchema(String type, boolean server, Locale locale) throws JsonException {
        switch (type) {
            case BLOCK:
            case BLOCKS:
                return doSchema(type,
                        server,
                        "jmri/server/json/block/block-server.json",
                        "jmri/server/json/block/block-client.json");
            default:
                throw new JsonException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Bundle.getMessage(locale, "ErrorUnknownType", type));
        }
    }
}
