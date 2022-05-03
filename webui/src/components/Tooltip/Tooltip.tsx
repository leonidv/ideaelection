import { Tooltip as TooltipMaterial, TooltipProps } from "@material-ui/core/"
import InfoOutlinedIcon from '@material-ui/icons/InfoOutlined'

const BootstrapTooltip:React.FC<TooltipProps> = (props: TooltipProps) => {
  return <TooltipMaterial arrow {...props} />
}

export const Tooltip = (props) => {
  const { title } = props

  return (
    <BootstrapTooltip title={title}>
      <InfoOutlinedIcon />
    </BootstrapTooltip>
  )
}
